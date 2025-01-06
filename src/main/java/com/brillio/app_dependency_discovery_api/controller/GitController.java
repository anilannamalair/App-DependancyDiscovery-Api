package com.brillio.app_dependency_discovery_api.controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.brillio.app_dependency_discovery_api.service.GitService;

@RestController
@RequestMapping("/api/git")
public class GitController {

    @Autowired
    private GitService gitService;

    // Clone repository
    @PostMapping("/clone")
    public String cloneRepository(@RequestParam String repoUrl) {
        try {
            gitService.cloneRepository(repoUrl);
            return "Repository cloned successfully";
        } catch (Exception e) {
            return "Error cloning repository: " + e.getMessage();
        }
    }

    // Get all applications in the repository
    @GetMapping("/applications")
    public List<String> getApplications(@RequestParam String repoUrl) {
        return gitService.getApplications(repoUrl);
    }

    // Get dependencies for a specific application
    @GetMapping("/dependencies")
    public String getDependencies(@RequestParam String repoUrl,@RequestParam String appName) {
        try {
            return gitService.getDependencies(appName,repoUrl);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching dependencies", e);
        }
    }
    
    @PostMapping("/assessment")
    public ResponseEntity<Map<String, Object>> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Process the CSV file using the GitService method
            List<Map<String, Object>> repoDetailsList = gitService.processCsvFile(file);

            // Populate the response
            response.put("status", "success");
            response.put("message", "File processed successfully!");
            response.put("repoDetails", repoDetailsList);  // Include the repo details in the response

            // Return a 200 OK response with the success message and data
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();

            // In case of an error, return a response with an error status
            response.put("status", "error");
            response.put("message", "Error processing file: " + e.getMessage());

            // Return a 500 Internal Server Error response
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
