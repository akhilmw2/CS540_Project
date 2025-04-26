// ExecutionRequest.java
package com.cs540.code_service.model;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ExecutionRequest {
    @NotBlank private String code;
    @NotBlank private String language; // "java", "python", "cpp"
    private String input;  // Optional for direct execution
    @NotBlank private String userId;
    @NotBlank private String problemId;
    private int timeoutSeconds = 5;

    // No need for explicit getter - @Data handles it
}