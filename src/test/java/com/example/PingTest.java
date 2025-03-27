package com.example;


import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PingTest {

    @Test
    public void testHealthCheck() {
        Response response = RestAssured.get("/ping");

        assertEquals(201, response.getStatusCode(), "Health check failed!");
    }
}
