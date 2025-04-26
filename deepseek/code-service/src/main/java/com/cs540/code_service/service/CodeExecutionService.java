package com.cs540.code_service.service;

import com.cs540.code_service.model.*;
import com.cs540.code_service.repository.SubmissionRepository;
import com.cs540.code_service.repository.TestCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Value("${execution.max-timeout-seconds:5}")
    private int defaultTimeout;

    @Value("${execution.workspace:/tmp/code_execution}")
    private String workspacePath;

    private ExecutionResult mapToExecutionResult(ProcessExecutionResult processResult) {
        ExecutionResult result = new ExecutionResult();
        result.setOutput(processResult.getOutput());
        result.setError(processResult.getError());
        result.setExecutionTime(processResult.getExecutionTime());
        result.setMemoryUsage(processResult.getMemoryUsage());
        result.setStatus(determineStatus(processResult));
        return result;
    }

    private String determineStatus(ProcessExecutionResult processResult) {
        if (processResult.isTimeout()) {
            return "TIMEOUT";
        } else if (processResult.getExitCode() != 0) {
            return processResult.getError().contains("error") ? "COMPILATION_ERROR" : "RUNTIME_ERROR";
        }
        return "SUCCESS";
    }

    private String determineTestCaseStatus(ExecutionResult executionResult, TestCase testCase) {
        if (!"SUCCESS".equals(executionResult.getStatus())) {
            return executionResult.getStatus();
        }

        return normalizeOutput(executionResult.getOutput()).equals(normalizeOutput(testCase.getExpectedOutput()))
                ? "PASSED" : "FAILED";
    }

    private String normalizeOutput(String output) {
        return output != null ? output.trim().replaceAll("\\r\\n", "\n") : "";
    }

    private void saveSubmission(SubmissionRequest request, SubmissionResult result) {
        Submission submission = new Submission();
        submission.setUserId(request.getUserId());
        submission.setProblemId(request.getProblemId());
        submission.setCode(request.getCode());
        submission.setLanguage(request.getLanguage());
        submission.setTimestamp(new Date());
        submission.setScore(result.getScore());
        submission.setStatus(result.getOverallStatus());
        submission.setExecutionResults(result.getTestCaseResults());

        submissionRepository.save(submission);
    }

    public ExecutionResult executeCode(ExecutionRequest request) {
        try {
            // Create workspace directory
            Path workspace = Paths.get(workspacePath);
            Files.createDirectories(workspace);

            // Prepare files
            String filename = prepareCodeFile(workspace, request.getCode(), request.getLanguage());

            // Execute code
            ProcessExecutionResult processResult = executeProcess(
                    getExecutionCommand(filename, request.getLanguage()),
                    request.getInput(),
                    request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : defaultTimeout
            );

            // Clean up
            cleanupWorkspace(workspace);

            return mapToExecutionResult(processResult);
        } catch (Exception e) {
            ExecutionResult errorResult = new ExecutionResult();
            errorResult.setStatus("ERROR");
            errorResult.setError(e.getMessage());
            return errorResult;
        }
    }

    public SubmissionResult submitCode(SubmissionRequest request) {
        // Get test cases (visible + hidden)
        List<TestCase> visibleTestCases = testCaseRepository.findByProblemIdAndHidden(request.getProblemId(), false);
        List<TestCase> allTestCases = new ArrayList<>();

        if (visibleTestCases != null) {
            allTestCases.addAll(visibleTestCases);
        }

        if (request.getHiddenTestCases() != null) {
            allTestCases.addAll(request.getHiddenTestCases());
        }

        // Handle empty test cases scenario
        if (allTestCases.isEmpty()) {
            return createEmptySubmissionResult();
        }

        List<TestCaseResult> results = processTestCases(request, allTestCases);
        return createSubmissionResult(results);
    }

    private SubmissionResult createEmptySubmissionResult() {
        SubmissionResult result = new SubmissionResult();
        result.setTestCaseResults(Collections.emptyList());
        result.setPassedCount(0);
        result.setTotalCount(0);
        result.setScore(0.0);
        result.setOverallStatus("NO_TEST_CASES");
        result.setAllPassed(false);
        return result;
    }

    private List<TestCaseResult> processTestCases(SubmissionRequest request, List<TestCase> testCases) {
        List<TestCaseResult> results = new ArrayList<>();

        for (TestCase testCase : testCases) {
            ExecutionRequest executionRequest = createExecutionRequest(request, testCase);
            ExecutionResult executionResult = executeCode(executionRequest);

            TestCaseResult testCaseResult = createTestCaseResult(testCase, executionResult);
            results.add(testCaseResult);
        }

        return results;
    }

    private ExecutionRequest createExecutionRequest(SubmissionRequest request, TestCase testCase) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setCode(request.getCode());
        executionRequest.setLanguage(request.getLanguage());
        executionRequest.setInput(testCase.getInput());
        executionRequest.setTimeoutSeconds(testCase.getTimeoutSeconds());
        executionRequest.setUserId(request.getUserId());
        executionRequest.setProblemId(request.getProblemId());
        return executionRequest;
    }

    private TestCaseResult createTestCaseResult(TestCase testCase, ExecutionResult executionResult) {
        boolean passed = isTestCasePassed(testCase, executionResult);

        return new TestCaseResult(
                testCase.getInput(),
                testCase.getExpectedOutput(),
                executionResult.getOutput(),
                passed ? "PASSED" : "FAILED",
                executionResult.getExecutionTime(),
                executionResult.getMemoryUsage(),
                executionResult.getError(),
                testCase.isHidden()
        );
    }

    private boolean isTestCasePassed(TestCase testCase, ExecutionResult executionResult) {
        return "SUCCESS".equals(executionResult.getStatus()) &&
                normalizeExecutionOutput(executionResult.getOutput())
                        .equals(normalizeTestCaseOutput(testCase.getExpectedOutput()));
    }

    private SubmissionResult createSubmissionResult(List<TestCaseResult> results) {
        int passedCount = calculatePassedCount(results);
        int totalCount = results.size();
        double score = calculateScore(passedCount, totalCount);
        String overallStatus = determineOverallStatus(passedCount, totalCount);
        boolean allPassed = (passedCount == totalCount);

        SubmissionResult submissionResult = new SubmissionResult();
        submissionResult.setTestCaseResults(results);
        submissionResult.setPassedCount(passedCount);
        submissionResult.setTotalCount(totalCount);
        submissionResult.setScore(score);
        submissionResult.setOverallStatus(overallStatus);
        submissionResult.setAllPassed(allPassed);

        return submissionResult;
    }

    private int calculatePassedCount(List<TestCaseResult> results) {
        return (int) results.stream()
                .filter(r -> "PASSED".equals(r.getStatus()))
                .count();
    }

    private double calculateScore(int passedCount, int totalCount) {
        return totalCount == 0 ? 0.0 : ((double) passedCount / totalCount) * 100;
    }

    private String determineOverallStatus(int passedCount, int totalCount) {
        return totalCount == 0 ? "NO_TEST_CASES" :
                passedCount == totalCount ? "ACCEPTED" : "REJECTED";
    }

    // Renamed to avoid duplicate method
    private String normalizeExecutionOutput(String output) {
        return output != null ? output.trim().replaceAll("\\r\\n", "\n") : "";
    }

    // Separate method for test case output normalization
    private String normalizeTestCaseOutput(String output) {
        return output != null ? output.trim().replaceAll("\\r\\n", "\n") : "";
    }

    private String prepareCodeFile(Path workspace, String code, String language) throws IOException {
        String filename = getFilename(language);
        Path filePath = workspace.resolve(filename);
        Files.write(filePath, code.getBytes());
        return filePath.toString();
    }

    private ProcessExecutionResult executeProcess(String command, String input, int timeoutSeconds) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();

        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }

        Process process = processBuilder.start();

        // Write input if provided
        if (input != null && !input.isEmpty()) {
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(input.getBytes());
                outputStream.flush();
            }
        }

        // Read output and error streams
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                error.append("Error reading output: ").append(e.getMessage());
            }
        });

        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            } catch (IOException e) {
                error.append("Error reading error stream: ").append(e.getMessage());
            }
        });

        outputThread.start();
        errorThread.start();

        // Wait for process to complete with timeout
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            return new ProcessExecutionResult(true, -1, output.toString(), "Execution timed out after " + timeoutSeconds + " seconds");
        }

        outputThread.join(1000);
        errorThread.join(1000);

        long executionTime = System.currentTimeMillis() - startTime;

        return new ProcessExecutionResult(
                false,
                process.exitValue(),
                output.toString(),
                error.toString(),
                executionTime,
                // TODO: Implement actual memory measurement
                0
        );
    }

    private void cleanupWorkspace(Path workspace) throws IOException {
        Files.walk(workspace)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Log warning
                    }
                });
    }

    private String getFilename(String language) {
        switch (language) {
            case "java": return "Main.java";
            case "python": return "main.py";
            case "cpp": return "main.cpp";
            default: throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    private String getExecutionCommand(String filename, String language) {
        Path filePath = Paths.get(filename);
        Path parent = filePath.getParent();
        String baseName = filePath.getFileName().toString().replaceFirst("[.][^.]+$", "");

        switch (language) {
            case "java":
                return "javac " + filename + " && java -cp " + parent + " " + baseName;
            case "python":
                return "python " + filename;
            case "cpp":
                String executable = parent.resolve(baseName).toString();
                return "g++ " + filename + " -o " + executable + " && " + executable;
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }
}