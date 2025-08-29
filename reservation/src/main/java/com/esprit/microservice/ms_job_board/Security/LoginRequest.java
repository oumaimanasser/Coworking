package com.esprit.microservice.ms_job_board.Security;

public class LoginRequest {
    private String username;
    private String password;

    // Default constructor
    public LoginRequest() {}

    // Parameterized constructor
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}