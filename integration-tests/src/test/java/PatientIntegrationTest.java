import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004"; // api gateway
    }

    @Test
    public void shouldReturnOkWithPatients() {

        String loginPayload = """
                {
                  "email": "testuser@test.com",
                  "password": "password123"
                }
                """.stripIndent().trim();

        String token = given() // arrange
                .contentType("application/json")
                .body(loginPayload)
                .when() // act
                .post("/auth/login")
                .then() // assert
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .jsonPath()
                .get("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                .body("patients", notNullValue());

    }
}

