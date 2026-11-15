# Codebeispiele für `quarkus-test`

Diese Notizen beziehen sich auf den aktuellen Projektstand im Paket `info.unterrainer.htl`. Sie verwenden die vorhandenen Klassen `StudentResource`, `Student`, den CDI-Dienst und JUnit/REST-assured.

> Nur zum Lernen vor dem Test. Du hast gesagt, dass KI während des Tests nicht verwendet werden darf; verwende KI-generierte Notizen oder Code morgen im Test nur, falls die Lehrkraft sie ausdrücklich erlaubt.

## So ist das Repo aufgebaut

```text
src/main/java/info/unterrainer/htl/
  StudentResource.java             # HTTP-/REST-Schicht
  Student.java                     # JSON-Datenobjekt mit Lombok
  service/DatabaseService.java     # CDI-Bean mit In-Memory-Map

src/test/java/info/unterrainer/htl/
  StudentResourceTest.java                  # REST-Erfolgsfälle
  StudentResourceMockServiceTest.java       # Spy-Beispiel
  StudentResourceErrorExamplesTest.java     # ergänzende 404-Fälle
```

Der CDI-Dienst ist mit `@ApplicationScoped` markiert und verwaltet die Student-Daten in einer `Map<Long, Student>`. `reset()` setzt die Map und den ID-Zähler zurück und legt zwei Beispiel-Studierende mit IDs 1 und 2 an. Die REST-Tests rufen diese Methode in `@BeforeEach` auf, damit jeder Test mit demselben Zustand startet.

## Beispiel: Endpunkte aus `StudentResource`

Die Resource stellt diese HTTP-Operationen bereit:

```text
GET    /students       -> Liste, 200
GET    /students/{id}  -> einzelner Student, 200 oder 404
POST   /students       -> Student anlegen, 201 + Location
PUT    /students/{id}  -> vorhandenen Student ändern, 200 oder 404
DELETE /students/{id}  -> löschen, 204 oder 404
```

Die Klasse erhält den Dienst per `@Inject`. Das ist CDI: Quarkus verwaltet die Bean und stellt sie der Resource bereit. Die Resource spricht HTTP, während der Dienst den Zugriff auf die Map und die CRUD-Operationen kapselt.

## Beispiel: POST-Request und `Location` testen

Ein Test kann einen Student ohne ID senden. Die aktuelle Resource sucht die höchste vorhandene ID und setzt die nächste ID. Nach dem Reset sind 1 und 2 belegt, daher ist die nächste ID 3.

```java
Student payload = Student.builder()
        .fullName("Alex Example")
        .email("alex@example.com")
        .build();

String location = given()
        .contentType("application/json")
        .body(payload)
    .when()
        .post("/students")
    .then()
        .statusCode(201)
        .header("Location", notNullValue())
        .body("id", is(3))
        .extract()
        .header("Location");

// Optionaler Folgerequest: Ressource über die zurückgegebene URI wieder lesen.
given()
    .when()
        .get(location)
    .then()
        .statusCode(200)
        .body("email", is("alex@example.com"));
```

`given()` bereitet Request und Body vor, `when()` sendet den Request, `then()` prüft die Response. `201 Created` zeigt die Erstellung an. Der `Location`-Header nennt die neue Ressource.

## Ergänzende 404-Codebeispiele

Die Datei `StudentResourceErrorExamplesTest.java` ergänzt drei Fälle, die im bestehenden `StudentResourceTest` noch nicht abgedeckt sind: unbekannte ID bei GET, DELETE und PUT. Jeder Test setzt zuvor den In-Memory-Datensatz zurück. Das hilft zu sehen, dass Fehlerfälle API-Verhalten sind, das genauso getestet werden sollte wie Erfolg.

Beispiel GET mit unbekannter ID:

```java
given()
  .when()
    .get("/students/999")
  .then()
    .statusCode(404)
    .body("error", is("Student not found"));
```

## Bestehende Mock-/Spy-Datei: Lernhinweise

`StudentResourceMockServiceTest` nutzt aktuell `@InjectSpy`. Ein Spy verwendet die echte Bean und beobachtet beziehungsweise überschreibt ausgewählte Methoden. Wenn das Lernziel ist, die echte Service-Bean vollständig zu ersetzen, ist `@InjectMock` geeigneter. Beim Spy ist für das Überschreiben des Rückgabewerts häufig `doReturn(...).when(spy).method(...)` nötig, da `when(spy.method())` die echte Methode zuerst aufrufen kann.

In `getById_resturnsFirstSeeded()` fallen drei konkrete Stellen auf, die ein Testergebnis verfälschen können:

1. `any(Long.class)` wird an eine Methode mit primitiver `long`-Signatur übergeben. Verwende dafür den primitiven Matcher `anyLong()`.
2. Der JSON-Feldname lautet `fullName`, nicht `fullname`.
3. Der gestubbte E-Mail-Wert ist als `gerald@unterrainder.info` geschrieben, während der erwartete beziehungsweise reale Wert `gerald@unterrainer.info` lautet.

So sieht die Korrektur der Assertions aus, wenn du für den Test einen passenden Student stubbst:

```java
.body("fullName", is("Gerald Unterrainer"))
.body("email", is("gerald@unterrainer.info"));
```

Diese Hinweise sind Beobachtungen am Code, keine bereits angewandte Änderung. Die vorhandene `StudentResourceMockServiceTest.java` bleibt unangetastet.

## Architekturbeobachtung am aktuellen Code

Der nächste gute Refactoring-Schritt wäre, die ID-Vergabe aus `StudentResource.createStudent()` in die Service-Schicht zu verschieben. Die Resource berechnet derzeit selbst `maxId + 1`, obwohl der Dienst bereits einen Zähler und `inc()` besitzt. Wenn die ID-Vergabe zentral im Service liegt, bleibt die Resource stärker auf HTTP beschränkt, und die Service-Regel lässt sich separat testen.

Ein mögliches Muster wäre:

```java
// Konzeptskizze für den Service
public Student create(Student incoming) {
    incoming.setId(inc());
    students.put(incoming.getId(), incoming);
    return incoming;
}
```

Dann delegiert die Resource an `ds.create(incoming)` und baut aus dem zurückgegebenen Objekt die `201`-Response. Die Skizze ist **kein** bereits eingebauter Code. Sie zeigt die Verantwortlichkeitsteilung und müsste an die konkrete API eurer Klasse angepasst werden.

## Ausführen

Im Repository liegt `mvnw`, aber beim Klonen fehlte `.mvn/wrapper/maven-wrapper.properties`; außerdem ist `mvn` in der aktuellen Umgebung nicht installiert. Daher konnte ich die Tests hier nicht starten. Mit einer funktionierenden Maven-Installation und den nötigen Java-Voraussetzungen kannst du im Repo `mvn test` ausführen. Die Tests in dieser Datei sind ergänzend; vorhandene Fehler in anderen Testklassen können den gesamten Lauf trotzdem fehlschlagen lassen.

## Projektdateien

- [StudentResource](../src/main/java/info/unterrainer/htl/StudentResource.java)
- [DatabaseService](../src/main/java/info/unterrainer/htl/service/DatabaseService.java)
- [Student](../src/main/java/info/unterrainer/htl/Student.java)
- [Bestehende REST-assured-Tests](../src/test/java/info/unterrainer/htl/StudentResourceTest.java)
- [Zusätzliche Fehlerfall-Tests](../src/test/java/info/unterrainer/htl/StudentResourceErrorExamplesTest.java)
