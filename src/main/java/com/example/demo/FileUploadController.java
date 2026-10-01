package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class FileUploadController {

    private final GitHubService gitHubService;

    private final Path uploadDirectory =
            Path.of("uploads");

    public FileUploadController(
            GitHubService gitHubService) {

        this.gitHubService = gitHubService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("environment") String environment,
            Model model) {

        try {

            // ------------------------------------
            // 1. Validate file
            // ------------------------------------

            if (file.isEmpty()) {

                model.addAttribute(
                        "message",
                        "Please select an Excel file."
                );

                return "index";
            }

            // ------------------------------------
            // 2. Generate Execution ID
            // ------------------------------------

            String executionId =
                    ExecutionIdGenerator.generate();

            System.out.println(
                    "Execution ID: " + executionId
            );

            // ------------------------------------
            // 3. Create local execution folder
            // ------------------------------------

            Path executionDirectory =
                    uploadDirectory.resolve(executionId);

            Files.createDirectories(
                    executionDirectory
            );

            // ------------------------------------
            // 4. Save Excel locally
            // ------------------------------------

            String fileName = file.getOriginalFilename();

            if (fileName == null ||
                    !(fileName.toLowerCase().endsWith(".xlsx")
                            || fileName.toLowerCase().endsWith(".xls"))) {

                model.addAttribute(
                        "message",
                        "Only Excel files (.xlsx or .xls) are allowed."
                );

                model.addAttribute("success", false);

                return "index";
            }

            Path targetFile =
                    executionDirectory.resolve(fileName);

            file.transferTo(targetFile);

            System.out.println(
                    "File saved: " + targetFile
            );

            // ------------------------------------
            // 5. GitHub path
            // ------------------------------------

            String githubPath =
                    "input-data/"
                            + executionId
                            + "/"
                            + fileName;

            System.out.println(
                    "GitHub path: " + githubPath
            );

            // ------------------------------------
            // 6. Commit to GitHub
            // ------------------------------------

            gitHubService.uploadFile(
                    targetFile,
                    githubPath
            );

            gitHubService.triggerWorkflow(
                    executionId,
                    fileName,
                    environment
            );

            // ------------------------------------
            // 7. Send success response to UI
            // ------------------------------------

            model.addAttribute(
                    "success",
                    true
            );

            model.addAttribute(
                    "executionId",
                    executionId
            );

            model.addAttribute(
                    "fileName",
                    fileName
            );

            model.addAttribute(
                    "message",
                    "Excel successfully committed to GitHub."
            );

        } catch (Exception e) {

            e.printStackTrace();

            model.addAttribute(
                    "success",
                    false
            );

            model.addAttribute(
                    "message",
                    "Upload failed: "
                            + e.getMessage()
            );
        }

        return "index";
    }
}