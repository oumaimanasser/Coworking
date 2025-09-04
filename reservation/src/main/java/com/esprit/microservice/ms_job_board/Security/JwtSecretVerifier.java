package com.esprit.microservice.ms_job_board.Security;

import java.util.Base64;

public class JwtSecretVerifier {
    public static void main(String[] args) {
        try {
            String secret = "3OMzZaUP2xW6z0eGzP36Ph+1uGtysUX3uHt/gFnlRL0=";
            byte[] decoded = Base64.getDecoder().decode(secret);
            System.out.println("Decoded length: " + decoded.length); // Should print 32
            System.out.println("JWT Secret is valid!");
        } catch (Exception e) {
            System.err.println("Error decoding JWT secret: " + e.getMessage());
        }
    }
}