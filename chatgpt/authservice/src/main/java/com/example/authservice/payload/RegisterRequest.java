

package com.example.authservice.payload;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;

    // Getters and Setters
    public String getName() { return name; }   // ✅ this already exists here!
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
