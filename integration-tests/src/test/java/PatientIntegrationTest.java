import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PatientIntegrationTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004"; // api gateway
    }
    
    @Test
    public void shouldReturnOkWithPatients() {

        String loginPayload = getLoginPayload();

        String token = getToken(loginPayload);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                .body("patients", notNullValue());

    }

    @Test
    public void shouldReturn429AfterLimitExceeded() throws InterruptedException {
        String loginPayload = getLoginPayload();
        String token = getToken(loginPayload);

        int total = 10;
        int tooManyRequests = 0; // track how many requests are 429

        for(int i=1; i <= total; i+=1){
            Response response = RestAssured
                    .given()
                    .header("Authorization", "Bearer " + token)
                    .get("/api/patients");

            System.out.printf("Request %d -> Status Code :%d%n", i, response.statusCode());

            if(response.statusCode() == 429) tooManyRequests++;

            Thread.sleep(100);
        }

        assertTrue(tooManyRequests >= 1 , "Expected at least one request to be 429");
    }

    private static String getToken(String loginPayload) {
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
        return token;
    }

    private static String getLoginPayload() {
        String loginPayload = """
                {
                  "email": "testuser@test.com",
                  "password": "password123"
                }
                """.stripIndent().trim();
        return loginPayload;
    }


}

