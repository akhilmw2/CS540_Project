
package com.example.authservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    private ERole name;

    // Getters and Setters

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public ERole getName() { return name; }

    public void setName(ERole name) { this.name = name; }
}
