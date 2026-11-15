# Lernunterlage: Quarkus REST, CDI, Architektur ohne Datenbank und REST-assured

> **Für die Vorbereitung vor dem Test.** Du hast angegeben, dass im Test keine KI verwendet werden darf. Diese von KI zusammengestellte Unterlage deshalb nur zur Vorbereitung verwenden und nicht während des Tests öffnen oder daraus abschreiben, außer die Lehrkraft erlaubt ausdrücklich vorbereitete KI-Unterlagen. Für den Test gelten die Regeln deiner Lehrkraft.

## 1. Was laut Kursplan dazugehört

Im 4AHIF-Kursplan sind unter anderem **REST**, **CDI – Contexts and Dependency Injection**, **JUnit & REST-assured**, **Quarkus – REST and CDI** und **Quarkus – Testing with JUnit and REST Assured** verlinkt. Die beiden Übungen „First REST Service 1 und 2“ ergänzen das mit einem REST-Service ohne echte Datenbank, einem In-Memory-`Map`, CDI und Tests. [1] [2] [3] [4] [5] [6] [7] [8]

Die Beispiele unten verbinden diese Themen an einem kleinen Katalog. Die Namen „Book“ oder „Student“ sind austauschbar; wichtig ist, die Rollen der Klassen und den HTTP-Testablauf zu verstehen.

## 2. REST und HTTP: die Grundidee

**REST** bedeutet *Representational State Transfer*. Es ist ein Architekturstil für APIs, nicht einfach ein anderes Wort für HTTP. Eine REST-API stellt **Ressourcen** bereit. Jede Ressource wird über eine URL identifiziert und mit einer kleinen, einheitlichen Menge von HTTP-Methoden gelesen oder verändert. Typische Eigenschaften sind Client-Server-Trennung, zustandslose Requests, Ressourcen-URLs, einheitliche Schnittstelle und Cachebarkeit. [2]

- **Client-Server:** Client und Server sind getrennt und kommunizieren per Request und Response.
- **Stateless:** Der Server soll sich nicht auf vorherigen Request-Zustand verlassen. Alles Nötige für einen Request wird mitgesendet.
- **Ressource:** Zum Beispiel ein Buch oder die Sammlung aller Bücher. Eine URL benennt die Ressource, etwa `/books` oder `/books/42`.
- **Einheitliche Schnittstelle:** Die HTTP-Methode beschreibt die Art der Aktion; der Pfad benennt das Ziel.
- **Idempotenz:** Mehrfaches Senden desselben Requests hat denselben beabsichtigten Effekt wie einmaliges Senden. Im Kursmaterial gelten GET, PUT und DELETE als idempotent; POST und typischerweise PATCH nicht. Bei DELETE kann die Antwort beim zweiten Mal anders aussehen, obwohl der Zustand weiterhin „Ressource ist gelöscht“ bleibt. [2]

### HTTP-Methode, Ziel und typische Verwendung

- `GET /books` liest eine Liste. Query-Parameter können filtern oder paginieren, zum Beispiel `/books?author=Herbert&page=1`.
- `GET /books/42` liest eine einzelne Ressource. Eine ID gehört üblicherweise in den **Pfad**.
- `POST /books` erstellt eine Ressource. Die neuen Daten stehen gewöhnlich im JSON-Request-Body.
- `PUT /books/42` ersetzt die Ressource unter dieser ID vollständig. Wird eine Ressource neu angelegt, kann PUT je nach API-Design auch `201 Created` liefern.
- `DELETE /books/42` löscht die Ressource mit dieser ID.
- `PATCH /books/42` verändert nur Teile einer Ressource.

REST-Pfade bezeichnen üblicherweise Ressourcen in der Mehrzahl und enthalten keine Aktionswörter wie `/create` oder `/delete`. Daher ist `DELETE /books/42` sinnvoller als `GET /books/delete?id=42`. [2]

### Drei Arten von Eingaben im REST-Endpunkt

- **Path-Parameter** sind Teil des Ressourcenpfads, zum Beispiel `{id}` in `/books/{id}`. In Jakarta REST liest `@PathParam("id")` den Wert.
- **Query-Parameter** stehen nach `?`, zum Beispiel `?author=Herbert`. `@QueryParam("author")` liest ihn.
- **Request-Body** trägt strukturierte Daten, meist JSON. Mit `@Consumes(MediaType.APPLICATION_JSON)` wird angegeben, dass der Endpunkt JSON annimmt.

### Häufige HTTP-Statuscodes im Stoff

- `200 OK`: erfolgreicher GET oder erfolgreiche Änderung mit Response-Body.
- `201 Created`: POST hat etwas neu erstellt; ein `Location`-Header sollte auf die neue Ressource zeigen.
- `204 No Content`: erfolgreich, aber kein Response-Body, häufig bei DELETE.
- `400 Bad Request`: Request ist ungültig, etwa wegen fehlgeschlagener Validierung.
- `404 Not Found`: die angeforderte ID existiert nicht.
- `500 Internal Server Error`: unerwarteter, nicht korrekt behandelter Serverfehler.

Im Unterrichtsbeispiel erzeugt `Response.created(uri).entity(saved).build()` eine `201`-Antwort mit `Location` und dem erstellten Objekt. `Response.noContent().build()` erzeugt `204`. [4]

## 3. Quarkus REST: wie eine Resource aussieht

Quarkus REST verwendet Jakarta-REST-Annotationen. `@Path` setzt einen Ressourcenpfad. `@GET`, `@POST`, `@PUT` und `@DELETE` ordnen Java-Methoden den HTTP-Methoden zu. `@Produces` bezeichnet das Antwortformat und `@Consumes` das erwartete Anfrageformat. [4]

```java
@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    private final BookService service;

    public BookResource(BookService service) {
        this.service = service;
    }

    @GET
    public List<Book> list(@QueryParam("author") String author) {
        return service.find(author);
    }

    @GET
    @Path("/{id}")
    public Book get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Response create(@Valid Book book) {
        Book saved = service.add(book);
        return Response.created(URI.create("/books/" + saved.id()))
                .entity(saved)
                .build();
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") long id) {
        service.remove(id);
    }
}
```

Die Klasse stellt folgende Verbindungen her:

- `GET /books` → `list(...)`
- `GET /books/42` → `get(42)`
- `POST /books` mit JSON-Body → `create(...)`
- `DELETE /books/42` → `delete(42)`

Ein `void`-Endpunkt kann im gezeigten Quarkus-Beispiel mit `204 No Content` antworten. Für einen nicht gefundenen Datensatz muss das Verhalten bewusst festgelegt werden: zum Beispiel eine Domain-Ausnahme werfen und diese auf `404` abbilden. [4]

### JSON und DTOs mit Records

Ein Java-`record` eignet sich als kompakter Datenübertragungs-Typ. Jackson kann JSON in das Objekt umwandeln und das Objekt wieder als JSON ausgeben. Bean-Validation-Annotationen können direkt an den Komponenten stehen. [4]

```java
public record Book(
        Long id,
        @NotBlank String title,
        @NotBlank String author,
        @Min(1450) int year) {
}
```

Mit `@Valid` am Request-Parameter wird der eingehende Inhalt validiert. Ist beispielsweise `title` leer oder `year` zu klein, liefert die Validierung typischerweise `400 Bad Request`, bevor der Methodenrumpf ausgeführt wird. Häufige Constraints sind `@NotBlank`, `@Min`, `@Size` und `@Email`. Dafür braucht das Projekt die passenden Quarkus-Erweiterungen für JSON und Bean Validation. [4]

### Fehler sauber in HTTP übersetzen

Die Business-Schicht soll nicht überall HTTP-Statuscodes zusammensetzen müssen. Sie kann eine fachliche Ausnahme wie `BookNotFoundException` werfen. Die REST-Schicht beziehungsweise ein Exception Mapper wandelt diese in eine HTTP-Antwort um. So bleibt die Fachlogik unabhängig von HTTP. [4]

```java
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(long id) {
        super("Book " + id + " not found");
    }
}

public class ErrorHandlers {
    @ServerExceptionMapper
    public RestResponse<ErrorDto> notFound(BookNotFoundException e) {
        return RestResponse.status(
                Response.Status.NOT_FOUND,
                new ErrorDto(e.getMessage()));
    }
}

public record ErrorDto(String message) { }
```

Ohne passenden Mapper kann eine unbehandelte Ausnahme zu `500` führen. Der Mapper macht aus einer fachlichen Ausnahme hier gezielt eine `404`-Antwort. [4]

## 4. Architektur ohne Datenbank

### Die Schichten und ihre Aufgaben

Für ein kleines API-Beispiel ist die Trennung in folgende Verantwortlichkeiten nützlich:

1. **Resource / Web-Schicht:** kennt HTTP, Pfade, Parameter, JSON, Statuscodes und delegiert die eigentliche Arbeit.
2. **Service / Fachlogik:** setzt Geschäftsregeln um, vergibt IDs und entscheidet, was bei fehlenden oder ungültigen Daten geschieht.
3. **Speicher-Schicht:** hält die Daten. Ohne Datenbank kann das eine Java-`Map` im Speicher sein.
4. **DTO / Model:** beschreibt die Daten, die im Programm beziehungsweise in JSON transportiert werden.

Ablauf beim Lesen: **HTTP-Request → Resource → Service → In-Memory-Speicher → Service → Resource → HTTP-Response**. Die Resource sollte nicht zugleich HTTP-Routing, Geschäftsregeln und Speicherverwaltung übernehmen.

### Was „ohne Datenbank“ im HTL-Beispiel konkret bedeutet

In „First REST Service 1“ wird statt einer Datenbank eine `Map<Long, Student>` als In-Memory-Speicher verwendet. Der Schlüssel ist die ID. Ein Zähler liefert neue IDs. Die Übung weist ausdrücklich darauf hin, dass eine Map direkt in der Resource keine gute endgültige Architektur ist; sie dient zunächst dazu, REST-Endpunkte ohne Datenbank aufzubauen. In Teil 2 wird der Speicher in eine eigene `DatabaseService`-Klasse ausgelagert. Trotz des Namens ist das weiterhin eine Map, also **keine echte Datenbank**. [7] [8]

Wichtige Folgen eines In-Memory-Speichers:

- Daten liegen nur im Prozessspeicher.
- Bei einem Neustart sind die Daten weg.
- Es gibt keine dauerhafte Speicherung und keine Datenbanktransaktionen.
- Tests müssen den Zustand kontrolliert zurücksetzen, sonst beeinflussen sich Tests gegenseitig.

### Beispiel für eine einfache Speicher-/Service-Schicht

```java
@ApplicationScoped
public class StudentService {
    private final Map<Long, Student> students = new HashMap<>();
    private long nextId = 1;

    public Collection<Student> getAll() {
        return students.values();
    }

    public Student get(long id) {
        return students.get(id);
    }

    public Student add(Student input) {
        Student saved = input.withId(nextId++); // sinngemäß: neues Objekt mit ID
        students.put(saved.id(), saved);
        return saved;
    }

    public boolean contains(long id) {
        return students.containsKey(id);
    }

    public void remove(long id) {
        students.remove(id);
    }

    public void resetForTest() {
        students.clear();
        nextId = 1;
        // optionale Testdaten hier neu einsetzen
    }
}
```

Der Code ist ein Architektur-Schema: Die genaue `Student`-Klasse kann je nach Verwendung ein `record`, ein Lombok-Objekt oder eine normale Java-Klasse sein. Im Kursbeispiel werden Methoden wie `getStudent`, `getStudents`, `addStudent`, `delStudent` und `containsStudent` eingeführt, damit die Resource nicht selbst direkt in die Map schreibt oder daraus löscht. [8]

### Warum Resource und Speicher nicht vermischt werden sollten

Wenn eine Resource selbst `new DatabaseService()` aufruft oder direkt `map.put(...)` und `map.remove(...)` ausführt, entstehen enge Kopplung und schwer testbarer Code. Wird der Service als Abhängigkeit übergeben, kann die Resource mit dem echten Service, einem Mock oder einer anderen Implementierung laufen. Die Übung führt genau diese Extraktion und anschließend das Mocking vor. [3] [8]

## 5. CDI: Contexts and Dependency Injection

**CDI** ist das System, das Beans erzeugt, verwaltet und ihre Abhängigkeiten miteinander verbindet. Eine **Bean** ist ein Objekt, dessen Erzeugung und Lebenszyklus der Container verwaltet. **Dependency Injection (DI)** bedeutet: Eine Klasse bekommt das, was sie braucht, von außen, statt es selbst mit `new` zu bauen. Das ist ein Beispiel für **Inversion of Control (IoC)**. [3] [4]

### Was bringt Dependency Injection?

Ohne DI:

```java
public class BookResource {
    private final BookService service = new BookService(new StudentMap());
}
```

Die Resource entscheidet selbst, welche Implementierung erzeugt wird. Das erschwert Austausch und Tests. Mit Constructor Injection:

```java
@ApplicationScoped
public class BookService {
    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }
}
```

Der Container verbindet die passenden Beans. Ein Test kann stattdessen einen Fake oder Mock bereitstellen. Im Quarkus-Beispiel ist Constructor Injection die bevorzugte Form; bei einer einzigen Konstruktor-Methode kann Quarkus `@Inject` weglassen. [4]

### Scopes: wie lange lebt eine Bean?

- `@ApplicationScoped`: eine Instanz für die Anwendung; praktisch der übliche Scope für Service-Beans. Quarkus stellt sie über einen Client-Proxy bereit.
- `@Singleton`: ebenfalls eine Instanz pro Anwendung, aber ohne den normalen CDI-Client-Proxy.
- `@RequestScoped`: eine Bean pro HTTP-Request.
- `@Dependent`: Standard-Scope, wenn kein anderer Scope angegeben ist; die Bean lebt abhängig von der Bean, in die sie injiziert wurde.

Ein REST-Resource-Objekt ist im Quarkus-Beispiel standardmäßig singleton-artig. Daher keine request-spezifischen Daten in Feldern der Resource ablegen. [3] [4]

### Häufige CDI-Annotationen

- `@Inject`: markiert eine Stelle, an der der Container eine passende Abhängigkeit bereitstellen soll.
- `@ApplicationScoped`, `@RequestScoped`, `@Dependent`: Lebensdauer / Scope einer Bean.
- `@Qualifier` oder `@Named`: wählt eine bestimmte Bean, wenn mehrere Implementierungen desselben Typs existieren. Ein eigener `@Qualifier` ist typsicherer als eine Zeichenkette.
- `@Produces`: erzeugt eine Bean über eine Methode, wenn sich die Klasse nicht selbst annotieren lässt oder die Erstellung besondere Logik braucht.
- `@Observes`: markiert eine Methode, die auf ein CDI-Event reagiert.
- `@PostConstruct`: Methode, die nach der Erzeugung und Injection der Bean aufgerufen wird.

### Der typische Fehler mit `new`

Wird eine Klasse mit `new` selbst erzeugt, weiß der CDI-Container nichts von diesem Objekt. Deshalb führt `@Inject` darin nicht zur erwarteten Injection. Beispiel: `new BookService()` erzeugt nicht automatisch eine CDI-verwaltete BookService-Bean. Nutze die vom Container verwaltete Instanz, üblicherweise per Constructor Injection. [3] [4]

Auch der Zeitpunkt ist wichtig: Bei Field Injection ist das Feld im Konstruktor zunächst noch nicht injiziert. Wenn Initialisierung von injizierten Abhängigkeiten abhängt, ist Constructor Injection passend. `@PostConstruct` ist eine Möglichkeit, nach der Injection Initialisierung auszuführen. Die Übung zeigt beide Varianten, empfiehlt für den allgemeinen, testbaren Entwurf aber Constructor Injection. [4] [8]

### CDI und Quarkus-Build

Quarkus analysiert Beans und Injection-Punkte beim Build. Eine fehlende oder mehrdeutige Bean kann dadurch schon als Build-Fehler auffallen. Werden zwei Beans desselben Typs gefunden, ist ein Qualifier nötig, damit klar ist, welche gemeint ist. [4]

## 6. JUnit und REST-assured

**JUnit Jupiter** strukturiert Java-Tests und führt sie aus. `@Test` markiert eine Testmethode; Assertions wie `assertEquals`, `assertNotNull`, `assertThrows` prüfen das Ergebnis. REST-assured ist eine Java-DSL zum Senden echter HTTP-Requests an eine API und zum Prüfen der Antworten. [5] [6]

### Die REST-assured-Grundformel

Merke dir **given → when → then**:

- `given()` – Request vorbereiten: Header, Content-Type, Query-Parameter oder Body.
- `when()` – HTTP-Request absenden: `get`, `post`, `put`, `delete`.
- `then()` – Antwort prüfen: Status, Header, Content-Type, JSON-Body.

```java
@QuarkusTest
class BookResourceTest {

    @Test
    void getBooksReturnsJson() {
        given()
          .when().get("/books")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             .body("size()", greaterThan(0));
    }
}
```

`@QuarkusTest` startet die Quarkus-Anwendung für den Test. REST-assured sendet HTTP-Requests an diese laufende Anwendung. In den Kursfolien läuft der Testmodus auf Port `8081`, während Dev Mode normalerweise `8080` verwendet. REST-assured wird von Quarkus passend auf den Test-Host und -Port konfiguriert. [6]

### JSON-Felder und Matcher prüfen

```java
given()
  .when().get("/books/1")
  .then()
     .statusCode(200)
     .body("author", equalTo("Herbert"))
     .body("title", equalTo("Dune"));
```

Der String im `body(...)` ist ein Ausdruck in den JSON-Antwortdaten. Bei verschachteltem JSON kann zum Beispiel `address.city` geprüft werden. Häufige Hamcrest-Matcher:

- `equalTo(value)` oder `is(value)` – gleicher Wert.
- `hasItem(value)` – Liste enthält einen Wert.
- `greaterThan(n)` – größer als `n`.
- `containsString(text)` – Zeichenkette enthält Text.
- `notNullValue()` – Wert ist nicht `null`.

Die Folien verwenden zusätzlich `size()` für die Zahl der Elemente in einer JSON-Liste. [5] [6]

### POST mit JSON-Body testen

```java
@Test
void createBookReturns201AndLocation() {
    given()
      .contentType(ContentType.JSON)
      .body(Map.of("title", "Dune", "author", "Herbert", "year", 1965))
    .when()
      .post("/books")
    .then()
      .statusCode(201)
      .header("Location", containsString("/books/"))
      .body("title", equalTo("Dune"));
}
```

`contentType(ContentType.JSON)` beschreibt das Anfrageformat. Der Body kann aus einem Java-Objekt oder einer Map bestehen. Eine Map ist besonders praktisch, wenn absichtlich ungültige Daten gesendet werden sollen, die ein streng validiertes DTO nicht zulässt. [6]

### Fehlerfälle mitprüfen

```java
@Test
void invalidBookReturns400() {
    given()
      .contentType(ContentType.JSON)
      .body(Map.of("title", "", "author", "Herbert", "year", 12))
    .when()
      .post("/books")
    .then()
      .statusCode(400);
}

@Test
void unknownIdReturns404() {
    given()
      .when().get("/books/9999")
      .then()
         .statusCode(404)
         .body("message", containsString("9999"));
}
```

Nicht nur den Erfolgsfall testen. Die Kursfolien empfehlen, für die Statuscodes, die eine Resource tatsächlich zurückgeben kann, Tests zu schreiben. [6]

### Zustand isolieren: Tests unabhängig machen

`@QuarkusTest` kann die Anwendung für mehrere Testklassen mit derselben Konfiguration gemeinsam verwenden. `@ApplicationScoped`-Beans und damit auch eine In-Memory-Map können zwischen Tests bestehen bleiben. Deshalb:

- Tests dürfen nicht von der Reihenfolge abhängen.
- Testdaten müssen reproduzierbar sein.
- Gemeinsamen Zustand vor jedem Test zurücksetzen, zum Beispiel mit `@BeforeEach`.
- Wenn ein Test einen neuen Datensatz erstellt, sollte er den eigenen erzeugten Datensatz anschließend über die zurückgegebene ID weiterverwenden, statt eine feste ID vorauszusetzen.

Die Übung setzt die Map und den ID-Zähler vor jedem Test zurück. [6] [7]

### Mocking: nur die gewünschte Schicht testen

`@InjectMock` ersetzt eine echte CDI-Bean in einem Quarkus-Test durch einen Mock. So kann etwa geprüft werden, ob die Resource eine Service-Ausnahme korrekt in `404` übersetzt, ohne gleichzeitig die echte Service-Logik oder den Speicher zu testen. Das `quarkus-junit-mockito`-Testmodul stellt diese Integration bereit. [6] [8]

Typisches Vorgehen mit Mockito:

```java
@InjectMock
BookService service;

@Test
void missingBookIsMappedTo404() {
    Mockito.when(service.get(7L))
           .thenThrow(new BookNotFoundException(7));

    given()
      .when().get("/books/7")
      .then()
         .statusCode(404);
}
```

Ein **Mock** ersetzt das Verhalten und gibt vorgegebene Werte zurück. Ein **Spy** beobachtet eine echte Instanz und kann etwa verifizieren, wie oft eine Methode aufgerufen wurde. Zu viele Mocks können ein Zeichen dafür sein, dass die Architektur schwer testbar oder zu eng gekoppelt ist. [8]

### Test-Pyramide, kurz

1. Viele schnelle **Unit-Tests** für reine Fachlogik, ohne Quarkus und ohne HTTP.
2. Einige **`@QuarkusTest`-Tests** für Integration und HTTP-Vertrag mit der laufenden Anwendung.
3. Wenige langsamere **`@QuarkusIntegrationTest`-Tests** gegen das gepackte Artefakt.

Für den beschriebenen Stoff ist besonders die Verbindung aus JUnit, `@QuarkusTest` und REST-assured relevant. `@QuarkusIntegrationTest` testet das gepackte Programm in einem separaten Prozess; dort funktionieren CDI-Injection und `@InjectMock` nicht wie in einem normalen `@QuarkusTest`. [6]

## 7. Häufige Prüfungsfehler vermeiden

- **HTTP-Methode und Pfad verwechseln:** `GET /books/delete?id=3` ist nicht das saubere REST-Muster; nutze `DELETE /books/3`.
- **ID im Query-Parameter statt im Pfad:** Bei einer einzelnen Ressource gehört die ID normalerweise in `/books/{id}`.
- **POST ohne passenden Status:** Bei erfolgreicher Erstellung ist `201 Created` samt `Location` passend.
- **`204` mit Body:** `No Content` heißt, dass die Response keinen Body enthält.
- **Validation vergessen:** `@NotBlank` am DTO allein genügt nicht für Request-Validierung; im Resource-Aufruf muss auch `@Valid` stehen.
- **Business-Schicht mit HTTP koppeln:** Fachlogik wirft eine Domain-Ausnahme; der Mapper macht daraus `404` oder einen anderen HTTP-Status.
- **Resource erzeugt Service mit `new`:** Dadurch umgehst du CDI und kannst Abhängigkeiten nicht sauber austauschen.
- **Map direkt in der Resource verändern:** Zum Verständnis möglich, aber besser in eine eigene Service-/Speicherklasse auslagern.
- **Tests setzen Reihenfolge voraus:** Jeder Test braucht eigenen Zustand oder einen Reset in `@BeforeEach`.
- **Nur Happy Path testen:** Auch ungültige Eingabe (`400`) und unbekannte ID (`404`) prüfen.
- **`@QuarkusTest` und Unit-Test verwechseln:** `@QuarkusTest` startet die Anwendung; ein Plain-JUnit-Test tut das nicht.

## 8. Mini-Übungen zur aktiven Wiederholung

Versuche die Fragen zuerst ohne die Antworten darunter zu beantworten.

1. Was macht `@Path("/books")`, und was macht `@Path("/{id}")` auf einer Methode?
2. Woher liest `@PathParam` seinen Wert? Woher `@QueryParam`?
3. Warum eignet sich `POST /books` zum Erstellen, aber `POST /books/create` nicht als bevorzugtes REST-Design?
4. Was ist der Unterschied zwischen `@ApplicationScoped` und `@RequestScoped`?
5. Warum reicht `new BookService(...)` nicht aus, wenn die Klasse CDI-Injection erwartet?
6. Was ist die Rolle von `@Valid` und welcher Statuscode passt bei ungültiger Eingabe?
7. Was bedeuten `given`, `when` und `then` in REST-assured?
8. Wie prüfst du in REST-assured eine `201`-Antwort und den `Location`-Header?
9. Warum muss eine In-Memory-Map vor Tests zurückgesetzt werden?
10. Was ist der Zweck von `@InjectMock`?

<details>
<summary>Antworten zum Selbstcheck</summary>

1. `@Path` auf der Klasse ist die Basis-URL; der Pfad auf der Methode ergänzt sie, zum Beispiel `/books/{id}`.
2. `@PathParam` liest einen Platzhalter aus dem Pfad; `@QueryParam` liest einen Wert nach `?`.
3. Der Pfad benennt die Ressource, die HTTP-Methode beschreibt die Aktion. POST auf die Sammlung erstellt ein neues Element.
4. `@ApplicationScoped` lebt für die Anwendung; `@RequestScoped` wird pro HTTP-Request neu bereitgestellt.
5. Ein selbst erzeugtes Objekt ist nicht automatisch vom CDI-Container verwaltet. Injection und Container-Lifecycle greifen dann nicht so, wie erwartet.
6. `@Valid` löst die Validierung des Request-Objekts aus; ungültige Eingabe führt typischerweise zu `400 Bad Request`.
7. `given` bereitet den Request vor, `when` sendet ihn, `then` prüft die Antwort.
8. `statusCode(201)` und `header("Location", notNullValue())` oder ein String-Matcher prüfen Status und Header.
9. Sonst hängt das Ergebnis vom Testzustand oder der Ausführungsreihenfolge ab.
10. Der Mock ersetzt eine echte Bean, sodass ein Test nur die gewünschte Schicht prüft und vorgegebenes Verhalten erhält.

</details>

## 9. Testregeln, die du genannt hast

Du hast folgende organisatorischen Regeln übermittelt:

- Internet ist erlaubt, KI nicht. Ein KI-Prompt kann zu einer mündlichen Prüfung führen; Franklyn überwacht die Regeln.
- Auf dem Tisch dürfen Laptop, etwas zu trinken oder zu essen, Zettel und Kugelschreiber liegen. Sonst nichts.
- Handy und Uhr gehören in die Tasche oder zum Lehrertisch. Wird eines woanders gefunden, folgt laut deiner Nachricht eine mündliche Prüfung.
- Bei krankheitsbedingtem Fehlen ist eine ärztliche Bestätigung nötig; der Test wird als mündliche Prüfung nachgeholt.
- Die Tests werden auf Plagiate geprüft; bei zweifelhafter Herkunft folgt laut deiner Nachricht eine mündliche Prüfung.

Das sind deine Angaben, keine von mir unabhängig geprüften Schulregeln. Halte dich an die offiziellen Ansagen deiner Lehrkraft. Weil KI im Test ausdrücklich verboten ist, ist die sichere Grenze: **Diese Unterlage heute zum Lernen nutzen; morgen keine KI verwenden und diese KI-generierte Unterlage nur dann während des Tests verwenden, wenn die Lehrkraft das ausdrücklich erlaubt.**

<!-- References -->

[1]: https://htl.dev/md/4AHIF/26-27%20-%204AHIF%20-%20WS.md "4AHIF Kursplan"
[2]: https://htl.dev/md/presentations/Quarkus/REST.md "REST"
[3]: https://htl.dev/md/presentations/Quarkus/CDI%20-%20Contexts%20And%20Dependency%20Injection.md "CDI Contexts and Dependency Injection"
[4]: https://htl.dev/md/presentations/Quarkus/Quarkus%20-%20REST%20and%20CDI.md "Quarkus REST and CDI"
[5]: https://htl.dev/md/presentations/Quarkus/JUnit%20%26%20REST-assured.md "JUnit and REST Assured"
[6]: https://htl.dev/md/presentations/Quarkus/Quarkus%20-%20Testing%20with%20JUnit%20and%20REST%20Assured.md "Quarkus Testing with JUnit and REST Assured"
[7]: https://htl.dev/md/exercises/WMC/EX%20-%20Quarkus%20-%20First%20REST%20Service%201.md "First REST Service 1"
[8]: https://htl.dev/md/exercises/WMC/EX%20-%20Quarkus%20-%20First%20REST%20Service%202.md "First REST Service 2"


## Weitere Infos
für 1. 
 A1.1 ./mvnw quarkus:dev startet, GET /films liefert die Demodaten
(Beispielaufruf in docs/curl-beispiele.txt).
 A1.2 Ein Film mit leerem Titel, Dauer 0 und Altersfreigabe 21 wird mit
400 abgelehnt.
 A1.3 Mit STORAGE = FILE legt ein zweiter Start keine Demodaten
dazu (belegt in docs/curl-beispiele.txt).

das sind 3 tests
beim ersten testes du ob GET /films deine demo daten aus DemoData (in service package) richtig liefert.
beim zeiten machst du dann einen test mit leerem Titel dauer 0 und age rating 21 und schaust ob er 400 liefert.
