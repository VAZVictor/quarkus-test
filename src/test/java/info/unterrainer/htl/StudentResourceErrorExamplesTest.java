package info.unterrainer.htl;

import info.unterrainer.htl.service.DatabaseService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Additional REST Assured examples for the error paths of StudentResource.
 * Each test starts from the same deterministic in-memory data set.
 */
@QuarkusTest
class StudentResourceErrorExamplesTest {

    @Inject
    DatabaseService databaseService;

    @BeforeEach
    void resetInMemoryData() {
        databaseService.reset();
    }

    @Test
    void getUnknownStudentReturns404() {
        given()
          .when()
            .get("/students/999")
          .then()
            .statusCode(404)
            .body("error", is("Student not found"));
    }

    @Test
    void deleteUnknownStudentReturns404() {
        given()
          .when()
            .delete("/students/999")
          .then()
            .statusCode(404)
            .body("error", is("Student not found"));
    }

    @Test
    void putUnknownStudentReturns404() {
        Student replacement = Student.builder()
                .fullName("New Name")
                .email("new@example.com")
                .build();

        given()
          .contentType("application/json")
          .body(replacement)
          .when()
            .put("/students/999")
          .then()
            .statusCode(404)
            .body("error", is("Student not found"));
    }
}
