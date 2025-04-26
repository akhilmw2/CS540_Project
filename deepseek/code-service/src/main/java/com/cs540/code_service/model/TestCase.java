package com.cs540.code_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@Document(collection = "test_cases")
public class TestCase {
    @Id
    private String id;

    @NotBlank
    private String problemId;

    @NotBlank
    private String input;

    @NotBlank
    private String expectedOutput;

    @Min(1) @Max(30)
    private int timeoutSeconds = 5;

    private boolean hidden;
}