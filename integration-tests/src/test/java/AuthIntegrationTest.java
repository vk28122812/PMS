import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004"; // api gateway
    }

    /*

     REST ASSURED TEST STRUCTURE

     Step 1: arrange
     Step 2: act
     Step 3: assert

     */

    @Test
    public void shouldReturnOKWithValidToken(){

        String loginPayload = """
                {
                  "email": "testuser@test.com",
                  "password": "password123"
                }
                """.stripIndent().trim();

        System.out.println("Login payload: " + loginPayload);

        Response response = given() // arrange
                .contentType("application/json")
                .body(loginPayload)
                .when() // act
                .post("/auth/login")
                .then() // assert
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .response();

        System.out.println("Generated token: " + response.jsonPath().getString("token"));

    }

    @Test
    public void shouldReturnUnauthorizedOnInvalidToken(){

        String loginPayload = """
                {
                  "email": "invalid@test.com",
                  "password": "wrong"
                }
                """.stripIndent().trim();

        System.out.println("Login payload: " + loginPayload);

        given() // arrange
                .contentType("application/json")
                .body(loginPayload)
                .when() // act
                .post("/auth/login")
                .then() // assert
                .statusCode(401);


    }



}
