package info.unterrainer.htl;

import info.unterrainer.htl.service.DatabaseService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class StudentResourceTest {
    @Inject
    DatabaseService ds;

    @BeforeEach
    void reset() {
        ds.reset();
    }

    @Test
    void getAll_returnsTwoSeeded() {
        given()
                .when()
                .get("/students")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("size()", is(2))
                .body("[0].id", notNullValue());
    }


    @Test
    void getById_returnsFirstSeeded() {
        given()
                .when()
                .get("/students/1")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("id", is(1))
                .body("fullName", is("Gerald Unterrainer"))
                .body("email", is("gerald@unterrainer.info"));
    }

    @Test
    void post_createsId3_and_isFetchable() {
        Student payload =
                Student.builder()
                        .fullName("Max Smith")
                        .birthDate(LocalDateTime.of(2001, 9, 21, 0,0,0))
                        .email("max@example.com")
                        .build();

        String location =
                given()
                        .contentType("application/json")
                        .body(payload)
                        .when()
                        .post("/students")
                        .then()
                        .statusCode(201)
                        .header("Location", notNullValue())
                        .extract()
                        .header("Location");

        // Get new ID from location of the test we just made...
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        assertThat(id, is(3L));

        given()
                .when()
                .get("/students/" + id)
                .then()
                .statusCode(200)
                .body("id", is((int) id))
                .body("email", is("max@example.com"));
    }

    @Test
    void delete_removesId1_and_followupGetIs404() {
        given()
                .when()
                .delete("/students/1")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/students/1")
                .then()
                .statusCode(404);
    }

    @Test
    void put_replacesId1_and_returnsUpdated() {
        Student payload =
                Student.builder()
                        .id(1L)
                        .fullName("Gerald U.")
                        .email("gerald.u@example.com")
                        .birthDate(LocalDateTime.of(1975,5,2,0,0,0))
                        .build();

        given()
                .contentType("application/json")
                .body(payload)
                .when()
                .put("/students/1")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("id", is(1))
                .body("fullName", is("Gerald U."))
                .body("email", is("gerald.u@example.com"));
    }
}

