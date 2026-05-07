package com.supermarket.pos.salesapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for the Supermarket POS Sales API.
 * 
 * This API manages the complete lifecycle of sales transactions,
 * including creation, modification, payment processing, freezing,
 * cancellation, and returns.
 * 
 * @author Supermarket POS Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class SalesApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesApiApplication.class, args);
    }
}
