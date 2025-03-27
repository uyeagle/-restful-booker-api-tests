package com.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestfulBookerApiTests {

    private static String token;
    private static int bookingId;

    private final String BASE_URL = "http://restful-booker.herokuapp.com";

    @BeforeAll
     static void setupAuth() {
        Map<String, String> authInfo = new HashMap<>();
        authInfo.put("username", "admin");
        authInfo.put("password", "password123");

        Response authResponse = RestAssured.given()
                .baseUri("http://restful-booker.herokuapp.com")
                .contentType(ContentType.JSON)
                .body(authInfo)
                .when()
                .post("/auth");

        assertEquals(200, authResponse.getStatusCode());
        assertTrue(authResponse.getBody().asString().contains("token"));
        token = authResponse.jsonPath().getString("token");
        System.out.println("Alınan Token: " + token);
    }
    @BeforeEach
    void checkTokenValidity() {
        assertNotNull(token, "Token should not be null");
        System.out.println("Token Valid: " + token);
    }

    @Test
    @Order(1)
    void testCreateBooking() {
        Map<String, Object> booking = new HashMap<>();
        booking.put("firstname", "Jim");
        booking.put("lastname", "Brown");
        booking.put("totalprice", 111);
        booking.put("depositpaid", true);

        Map<String, String> bookingdates = new HashMap<>();
        bookingdates.put("checkin", "2018-01-01");
        bookingdates.put("checkout", "2019-01-01");
        booking.put("bookingdates", bookingdates);

        booking.put("additionalneeds", "Breakfast");

        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(booking)
                .when()
                .post("/booking");

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().asString().contains("bookingid"));
        assertTrue(response.getBody().asString().contains("booking"));
        assertEquals("Jim", response.jsonPath().getString("booking.firstname"));

        bookingId = response.jsonPath().getInt("bookingid");
        System.out.println("Oluşturulan Rezervasyon ID: " + bookingId);
        System.out.println("Oluşturulan Rezervasyonun Yanıtı:");
        System.out.println(response.prettyPrint());
    }

    @Test
    @Order(2)
    void testGetAllBookingIds() {
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .when()
                .get("/booking");

        assertEquals(200, response.getStatusCode());
        List<Integer> bookingIdsList = response.jsonPath().getList("bookingid");
        assertNotNull(bookingIdsList, "Dönen booking ID'leri listesi null olmamalı.");
        assertTrue(bookingIdsList.size() > 0, "API'de en az bir rezervasyon olmalı.");
        System.out.println("Tüm Booking ID'leri alındı. Toplam: " + bookingIdsList.size());
        System.out.println(response.prettyPrint());
    }

    @Test
    @Order(3)
    void testGetBookingById() {
        assertTrue(bookingId > 0, "Önce rezervasyon oluşturulmalı.");

        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .pathParam("id", bookingId)
                .header("Accept", "application/json")
                .when()
                .get("/booking/{id}");

        assertEquals(200, response.getStatusCode());
        assertEquals("Jim", response.jsonPath().getString("firstname"));
        assertEquals("Brown", response.jsonPath().getString("lastname"));
        System.out.println("ID (" + bookingId + ") ile Rezervasyon Detayları:");
        System.out.println(response.prettyPrint());
    }

    @Test
    @Order(4)
    void testUpdateExistingBooking() {
        assertTrue(bookingId > 0, "Önce rezervasyon oluşturulmalı.");
        assertNotNull(token, "Token alınmamış.");

        Map<String, Object> updatedBooking = new HashMap<>();
        updatedBooking.put("firstname", "James");
        updatedBooking.put("lastname", "Brown");
        updatedBooking.put("totalprice", 111);
        updatedBooking.put("depositpaid", true);

        Map<String, String> bookingdates = new HashMap<>();
        bookingdates.put("checkin", "2018-01-01");
        bookingdates.put("checkout", "2019-01-01");
        updatedBooking.put("bookingdates", bookingdates);

        updatedBooking.put("additionalneeds", "Breakfast");

        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                // Authorization (Cookie) başlığı ekleniyor
                .header("Cookie", "token=" + token)
                .pathParam("id", bookingId)
                .body(updatedBooking)
                .when()
                .put("/booking/{id}");

//      assertEquals(200, response.getStatusCode(), "Status code should be 200 for successful update");
        System.out.println("Güncellenen Rezervasyonun Yanıtı (ID: " + bookingId + "):");
        System.out.println(response.prettyPrint());
    }

    @Test
    @Order(5)
    void testPartialUpdateBooking() {
        // Check if bookingId and token are available
        assertTrue(bookingId > 0, "Booking ID should be created before updating");
        assertNotNull(token, "Token must be present for update");

        // Prepare the partial update body
        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("firstname", "James");
        partialUpdate.put("lastname", "Brown");

        // Send the PATCH request
        Response response = given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Cookie", "token=" + token)  // Authorization token
                .pathParam("id", bookingId)  // Using dynamic bookingId
                .body(partialUpdate)  // Request body with updated fields
                .patch("/booking/{id}");  // PATCH endpoint

        // Print the response body for debugging
        System.out.println("Response: " + response.prettyPrint());

//      Assert the response status code and the updated fields
//      assertEquals(200, response.getStatusCode(), "Status code should be 200 for successful update");
//      assertEquals("James", response.jsonPath().getString("firstname"));
//      assertEquals("Brown", response.jsonPath().getString("lastname"));
    }
    @Test
    @Order(6)
    void testDeleteBooking() {
        assertTrue(bookingId > 0, "Önce rezervasyon oluşturulmalı.");
        assertNotNull(token, "Token alınmamış.");

        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .header("Cookie", "token=" + token)
                .pathParam("id", bookingId)
                .when()
                .delete("/booking/{id}");

        assertEquals(201, response.getStatusCode());
        System.out.println("Rezervasyon (ID: " + bookingId + ") Silindi. Status Code: " + response.getStatusCode());

        Response getResponseAfterDelete = RestAssured.given()
                .baseUri(BASE_URL)
                .pathParam("id", bookingId)
                .header("Accept", "application/json")
                .when()
                .get("/booking/{id}");

        assertEquals(404, getResponseAfterDelete.getStatusCode());
        System.out.println("Silinen Rezervasyonun GET Sonucu (ID: " + bookingId + "): Status Code " + getResponseAfterDelete.getStatusCode());
    }

    @AfterAll
    static void cleanup() {
        System.out.println("Tüm testler tamamlandı.");
        // İstenirse, testler sonrasında temizleme işlemleri burada yapılabilir.
    }
}