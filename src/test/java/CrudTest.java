import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class CrudTest {

// Declaring variables to get token and booking id

    private String token;
    private int bookingId;

// Executes once before any test methods in the class

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        token = getToken("admin", "password123");
        Assert.assertNotNull(token);
    }

// To send post request with json body and validate if token is present in the response body else returns null

    private String getToken(String username, String password) {
        JSONObject crednetials = new JSONObject();
        crednetials.put("username", username);
        crednetials.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(crednetials.toString())
                .post("/auth");

        if (response.statusCode() == 200 && response.jsonPath().get("token") != null) {
            return response.jsonPath().getString("token");
        }
        return null;
    }
// To create a new booking with all the details and returns booking with json object

    private JSONObject createBooking(String fname, String lname) {
        JSONObject booking = new JSONObject();
        booking.put("firstname", fname);
        booking.put("lastname", lname);
        booking.put("totalprice", 500);
        booking.put("depositpaid", true);

        JSONObject bookingDates = new JSONObject();
        bookingDates.put("checkin", "2025-08-01");
        bookingDates.put("checkout", "2025-08-10");

        booking.put("bookingdates", bookingDates);
        booking.put("additionalneeds", "Breakfast");

        return booking;
    }

// Sends POST request to create a new booking and save the booking id

    @Test(priority = 1)
    public void CreateBooking_Positive() {
        JSONObject payload = createBooking("Atul", "Surve");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(payload.toString())
                .post("/booking");

        response.then().statusCode(200);
        bookingId = response.jsonPath().getInt("bookingid");
        Assert.assertTrue(bookingId > 0);
    }

// To verify the created booking is retrievable and also verify the name on which booking is done

    @Test(priority = 2)
    public void GetBooking_Positive() {
        given()
                .get("/booking/" + bookingId)
                .then()
                .statusCode(200)
                .body("firstname", equalTo("Atul"));
    }

// To update the existing booking and also authenticated with the token

    @Test(priority = 3)
    public void UpdateBooking_Positive() {
        JSONObject payload = createBooking("Sam", "Shaw");

        given()
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .body(payload.toString())
                .put("/booking/" + bookingId)
                .then()
                .statusCode(200)
                .body("firstname", equalTo("Sam"));
    }

// To delete the existing booking

    @Test(priority = 4)
    public void DeleteBooking_Positive() {
        given()
                .cookie("token", token)
                .delete("/booking/" + bookingId)
                .then()
                .statusCode(201);
    }

 // To verify if the deleted booking does not exist

    @Test(priority = 5)
    public void GetBooking_NotFound() {
        given()
                .get("/booking/" + bookingId)
                .then()
                .statusCode(404);
    }

//***********  NEGATIVE Scenarios **********

// To login with wrong credentials and no token is returned

    @Test(priority = 6)
    public void LoginWithInvalidCreds() {
        String invalidToken = getToken("wronguser", "wrongpass");
        Assert.assertNull(invalidToken, "Null token for invalid credentials");
    }

//  Creating a new booking and then trying to update it with invalid token

    @Test(priority = 7)
    public void UpdateWithInvalidToken() {
        JSONObject payload = createBooking("Greg", "Menwill");

        // First, create a booking again
        Response create = given()
                .contentType(ContentType.JSON)
                .body(payload.toString())
                .post("/booking");
        int tempBookingId = create.jsonPath().getInt("bookingid");

        // Attempt to update with invalid token
        given()
                .contentType(ContentType.JSON)
                .cookie("token", "invalidtoken123")
                .body(payload.put("firstname", "InvalidName").toString())
                .put("/booking/" + tempBookingId)
                .then()
                .statusCode(403); // Forbidden
    }

 // To delete booking which is non existent with bad token

    @Test(priority = 8)
    public void DeleteWithInvalidToken() {
        given()
                .cookie("token", "badToken123456")
                .delete("/booking/12098908")
                .then()
                .statusCode(403);
    }

// To update the booking which is non existent with right token

    @Test(priority = 9)
    public void UpdateNonExistBooking() {
        JSONObject payload = createBooking("Unknown", "User");

        given()
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .body(payload.toString())
                .put("/booking/334466778")
                .then()
                .statusCode(405);
    }

// To delete a booking which is non existent with right token
    @Test(priority = 10)
    public void DeleteNonExistentBooking() {
        given()
                .cookie("token", token)
                .delete("/booking/897656")
                .then()
                .statusCode(405); // Method Not Allowed
    }

}
