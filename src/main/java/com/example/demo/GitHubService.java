package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class GitHubService {

    private final RestClient restClient;

    private final String owner;
    private final String repository;
    private final String branch;

    public GitHubService(
            @Value("${github.owner}") String owner,
            @Value("${github.repository}") String repository,
            @Value("${github.branch}") String branch) {

        this.owner = owner;
        this.repository = repository;
        this.branch = branch;

        String token = System.getenv("GITHUB_PAT");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "GITHUB_TOKEN environment variable is not configured."
            );
        }

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        "application/vnd.github+json"
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public String uploadFile(
            Path localFile,
            String githubFilePath) throws Exception {

        // Read Excel file
        byte[] fileBytes = Files.readAllBytes(localFile);

        // Convert file to Base64
        String base64Content =
                Base64.getEncoder().encodeToString(fileBytes);

        // Create commit message
        String commitMessage =
                "Upload automation input: "
                        + localFile.getFileName();

        // GitHub API request body
        String requestBody = """
                {
                    "message": "%s",
                    "content": "%s",
                    "branch": "%s"
                }
                """.formatted(
                commitMessage,
                base64Content,
                branch
        );

        return restClient.put()
                .uri(
                        "/repos/{owner}/{repo}/contents/{path}",
                        owner,
                        repository,
                        githubFilePath
                )
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    public void triggerWorkflow(
            String executionId,
            String fileName,
            String environment
            ) {

        String requestBody = """
            {
                "ref": "%s",
                "inputs": {
                    "execution_id": "%s",
                    "file_name": "%s",
                    "environment": "%s"
                }
            }
            """.formatted(
                branch,
                executionId,
                fileName,
                environment
        );

        restClient.post()
                .uri(
                        "/repos/{owner}/{repo}/actions/workflows/{workflow}/dispatches",
                        owner,
                        repository,
                        "selenium-tests.yml"
                )
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}