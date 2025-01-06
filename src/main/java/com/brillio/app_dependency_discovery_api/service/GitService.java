package com.brillio.app_dependency_discovery_api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.brillio.app_dependency_discovery_api.config.Config;
import com.brillio.app_dependency_discovery_api.utilities.ApprovalGates;
import com.brillio.app_dependency_discovery_api.utilities.ArtifactManagement;
import com.brillio.app_dependency_discovery_api.utilities.CloudInfraAndContainerization;
import com.brillio.app_dependency_discovery_api.utilities.ComplexityClassifier;
import com.brillio.app_dependency_discovery_api.utilities.ErrorHandlingAndRetryLogic;
import com.brillio.app_dependency_discovery_api.utilities.ExternalIntegrations;
import com.brillio.app_dependency_discovery_api.utilities.NumberOfEnvironmentsOrDeployments;
import com.brillio.app_dependency_discovery_api.utilities.NumberOfLibraries;
import com.brillio.app_dependency_discovery_api.utilities.NumberOfParallelExecution;
import com.brillio.app_dependency_discovery_api.utilities.PipelineScriptComplexity;
import com.brillio.app_dependency_discovery_api.utilities.Repository;
import com.brillio.app_dependency_discovery_api.utilities.SecurityAndComplianceChecks;
import com.brillio.app_dependency_discovery_api.utilities.TestingAndCodeQualityChecks;
import com.brillio.app_dependency_discovery_api.utilities.TriggersDetails;
import com.brillio.app_dependency_discovery_api.utilities.VariablesFinder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Service
public class GitService {

	private static final String BASE_DIR = "C:/Users/Anil.Kumar4/repo"; // Base directory for cloning repos
	private String localDirectory;
	private String jenkinsfilePath;
	private Object serviceParamsMap; 
    Map<String, String> configFiles = new HashMap<>();
	// Clone the repository into a folder named after the repo
	public void cloneRepository(String repoUrl) throws Exception {
		// Extract the repository name from the URL
		String repoName = extractRepoNameFromUrl(repoUrl);
		if (repoName == null) {
			throw new IllegalArgumentException("Invalid repository URL.");
		}

		// Create a directory with the repository name inside the base directory
		File cloneDir = new File(BASE_DIR, repoName);
		System.out.println("Cloning repository into: " + cloneDir.getAbsolutePath());

		// Check if the repository is already cloned
		if (cloneDir.exists() && isGitRepository(cloneDir)) {
			System.out.println("Repository already cloned at: " + cloneDir.getAbsolutePath());
			return; // Skip cloning if already present
		}

		// Clone the repository into the newly created directory
		System.out.println("Cloning repository from: " + repoUrl);
		Git.cloneRepository().setURI(repoUrl).setDirectory(cloneDir).call();

		// Log the clone status
		if (cloneDir.exists() && cloneDir.isDirectory()) {
			System.out.println("Repository successfully cloned into: " + cloneDir.getAbsolutePath());
		} else {
			System.out.println("Failed to clone repository.");
		}
	}

	// Check if directory contains a .git folder to determine if it is a Git
	// repository
	private boolean isGitRepository(File directory) {
		File gitDir = new File(directory, ".git");
		return gitDir.exists() && gitDir.isDirectory();
	}

	// Traverse the cloned repository directory to find all applications
	public List<String> getApplications(String repoUrl) {
		List<String> applications = new ArrayList<>();

		// Extract the repository name from the URL
		String repoName = extractRepoNameFromUrl(repoUrl);
		if (repoName == null) {
			System.out.println("Invalid repository URL.");
			return applications;
		}

		// Construct the full path to the cloned repository
		File repoDir = new File(BASE_DIR, repoName);
		System.out.println("Looking for repository directory: " + repoDir.getAbsolutePath());

		// Check if the repository directory exists
		if (!repoDir.exists() || !repoDir.isDirectory()) {
			System.out.println("Repository directory not found at: " + repoDir.getAbsolutePath());
			return applications; // Return empty list if repository subdirectory is not found
		}

		// Start the recursive search for applications (directories containing pom.xml)
		findApplicationsInDirectory(repoDir, applications);

		if (applications.isEmpty()) {
			System.out.println("No applications found in the repository.");
		} else {
			System.out.println("Applications found: " + applications);
		}

		return applications;
	}

	// Recursive method to find applications (directories with pom.xml)
	private void findApplicationsInDirectory(File dir, List<String> applications) {
		if (dir == null || !dir.isDirectory())
			return;

		// If this directory contains a pom.xml file, it is considered an application
		if (containsPom(dir)) {
			// Ensure it's not a parent directory of multi-modules (root module)
			if (isLikelyApplicationDirectory(dir)) {
				// Add only the directory name (not the full path)
				applications.add(dir.getName());
			}
		}

		// Recursively check subdirectories
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				findApplicationsInDirectory(file, applications);
			}
		}
	}

	// Check if directory contains pom.xml
	private boolean containsPom(File dir) {
		File pomFile = new File(dir, "pom.xml");
		return pomFile.exists();
	}

	// Helper method to check if a directory is likely to be an application
	private boolean isLikelyApplicationDirectory(File dir) {
		// You can add more conditions based on your project's structure.
		// For example, check if the directory doesn't contain multiple modules in a
		// multi-module project.

		// For now, we'll assume that the directory containing pom.xml is a valid
		// application.
		return true;
	}

	// Get dependencies for a specific application (given appName)
	public String getDependencies(String appName, String repoUrl) throws IOException {
		// Extract the repository name from the URL
		String repoName = extractRepoNameFromUrl(repoUrl);
		if (repoName == null) {
			System.out.println("Invalid repository URL.");
			return null;
		}

		// Construct the full path to the cloned repository and the application
		// directory
		File appDir = new File(BASE_DIR, repoName + "/" + appName);
		System.out.println("Looking for dependencies in: " + appDir.getAbsolutePath());

		// Ensure the application directory exists
		if (!appDir.exists() || !appDir.isDirectory()) {
			System.out.println("Application directory not found: " + appDir.getAbsolutePath());
			return null; // Return null if the directory doesn't exist
		}

		// Now we will look for dependencies in this application's pom.xml file
		return getDependenciesFromPomFiles(appDir);
	}

	// Recursively gather dependencies from all pom.xml files in the given directory
	private String getDependenciesFromPomFiles(File dir) throws IOException {
		List<String> dependencies = new ArrayList<>();

		// Check if this is a directory
		if (dir == null || !dir.isDirectory()) {
			return null; // If it's not a directory, return null
		}

		// Look for pom.xml in this directory
		File pomFile = new File(dir, "pom.xml");
		if (pomFile.exists()) {
			// Read the content of the pom.xml file
			String xmlContent = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);

			try {
				// Initialize XmlMapper and ObjectMapper to parse XML to JSON
				XmlMapper xmlMapper = new XmlMapper();
				ObjectMapper jsonMapper = new ObjectMapper();

				// Convert XML content into a JsonNode
				JsonNode jsonNode = xmlMapper.readTree(xmlContent);

				// Optionally, you could filter or process the JSON to extract dependencies from
				// the pom.xml
				String jsonString = jsonMapper.writeValueAsString(jsonNode);

				// Add this JSON string to the dependencies list
				dependencies.add(jsonString);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Recursively check subdirectories for more pom.xml files (in case of
		// multi-module projects)
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				String subDependencies = getDependenciesFromPomFiles(file);
				if (subDependencies != null) {
					dependencies.add(subDependencies);
				}
			}
		}

		// Return a combined JSON representation of all dependencies found
		return dependencies.isEmpty() ? null : String.join(",", dependencies);
	}

	// Helper method to extract the repository name from the URL
	private String extractRepoNameFromUrl(String repoUrl) {
		// Assuming the URL is of the form: https://github.com/username/repo-name.git
		if (repoUrl == null || repoUrl.isEmpty()) {
			return null;
		}

		// Remove the ".git" suffix if present
		if (repoUrl.endsWith(".git")) {
			repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
		}

		// Get the part after the last "/"
		String[] parts = repoUrl.split("/");
		return parts.length > 0 ? parts[parts.length - 1] : null;
	}
	

	    // Method to process the CSV file and find Jenkinsfile in the repos
	    public List<Map<String, Object>> processCsvFile(MultipartFile file) throws IOException {
	        List<Map<String, Object>> repoDetailsList = new ArrayList<>();

	        // Read the CSV file using BufferedReader
	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
	            String line;
	            // Skip the header row if it exists (optional)
	            reader.readLine();  // Uncomment this if the first line is a header
	            
	            // Read each line from the file
	            while ((line = reader.readLine()) != null) {
	                // Split the line by commas (assuming comma-separated values)
	                String[] fields = line.split(",");
	                
	                // Check if we have the correct number of columns (repo_url, repo_access_token)
	                if (fields.length >= 2) {
	                    String repoUrl = fields[0].trim();  // repo_url is the first column
	                    String repoAccessToken = fields[1].trim();  // repo_access_token is the second column
	                    
	                    // Perform processing with the extracted data
	                   
	                    
	                    // Collect parameters for the current repo
	                    Map<String, Object> repoDetails = checkForJenkinsfile(repoUrl, repoAccessToken);
	                    repoDetailsList.add(repoDetails);
	                } else {
	                    System.out.println("Invalid line format: " + line);
	                }
	            }
	        } catch (IOException e) {
	            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
	        }

	        return repoDetailsList;
	    }

	    private Map<String, Object> checkForJenkinsfile(String repoUrl, String repoAccessToken) throws IOException {
	        // Create a temporary directory to clone the repository
	        File tempDir = new File(System.getProperty("java.io.tmpdir"), "repo_" + System.currentTimeMillis());
	        if (!tempDir.exists()) {
	            tempDir.mkdirs();
	        }

	        Map<String, Object> repoDetails = new HashMap<>();
	        repoDetails.put("repoUrl", repoUrl);

	        try {
	            // Clone the repo using JGit
	            Git git = Git.cloneRepository()
	                    .setURI(repoUrl)
	                    .setDirectory(tempDir)
	                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", repoAccessToken)) // Set credentials if needed
	                    .call();

	            // Recursively search for Jenkinsfiles in the cloned repository
	            File rootDir = tempDir;
	            List<File> jenkinsfiles = findJenkinsfiles(rootDir);

	            Map<String, Object> serviceParamsMap = new HashMap<>(); // To store service details by service name

	            if (!jenkinsfiles.isEmpty()) {
	                for (File jenkinsFile : jenkinsfiles) {
	                    String serviceName = getServiceName(jenkinsFile);  // Determine service name based on Jenkinsfile directory
	                    String localDirectory = jenkinsFile.getParentFile().getAbsolutePath();
	                    String jenkinsfilePath = jenkinsFile.getAbsolutePath();

	                    // Initialize service details map
	                    Map<String, Object> serviceDetails = new HashMap<>();
	                    serviceDetails.put("serviceName", serviceName);

	                    // Process Jenkinsfile content
	                    String jenkinsfileContent = new String(Files.readAllBytes(jenkinsFile.toPath()), StandardCharsets.UTF_8);

	                    // Process configuration files (Dockerfile, pom.xml, build.gradle, etc.)
	                    File serviceDir = jenkinsFile.getParentFile();

	                    // Call VariablesFinder methods after cloning the repo and getting the Jenkinsfiles
	                    List<String[]> variableContentLinesWithDollar = VariablesFinder.findLinesWithDollarSignEnvironmentVariables(localDirectory);
	                    int environmentVariablesLinesFilePath = VariablesFinder.writeOutputToFile(localDirectory, variableContentLinesWithDollar);
	                    Map<String, Integer> validVariables = VariablesFinder.findValidVariables(localDirectory);
	                    List<String> variablesList = new ArrayList<>(validVariables.keySet());

	                    // Count the number of valid variables
	                    int totalVariablesCount = variablesList.size();
	                    String variablesListStr = String.join(", ", variablesList);

	                    // Artifactory configuration
	                    ArtifactManagement.ArtifactoryResult artifactoryResult = ArtifactManagement.checkArtifactoryInJenkinsfile(jenkinsfileContent);
	                    boolean isArtifactoryPresent = artifactoryResult.isPresent;
	                    List<String> artifactoryLines = artifactoryResult.artifactoryLines;
	                    String artifactoryPresentMsg = isArtifactoryPresent ? "Yes" : "No";

	                    // Error handling and retry block counts
	                    int[] tryRetryResult = ErrorHandlingAndRetryLogic.countTryAndRetryBlocks(jenkinsfileContent);
	                    int tryCount = tryRetryResult[0];
	                    int retryCount = tryRetryResult[1];
	                    String tryAndRetryCountMsg = "Error handling count = " + tryCount + ", Retry count = " + retryCount;

	                    // Count the number of Approval stages found in the Jenkinsfile
	                    int approvalCount = ApprovalGates.countApprovalStages(jenkinsfileContent);

	                    // Count parallel execution blocks from Jenkinsfile
	                    int[] jobParallelResult = NumberOfParallelExecution.countJobsAndParallelBlocks(jenkinsfileContent);
	                    int jobCount = jobParallelResult[0];
	                    int parallelCount = jobParallelResult[1];

	                    // Get the list of libraries mentioned in lines that start with 'library'
	                    List<String> libraries = NumberOfLibraries.getLibrariesFromJenkinsfileContent(jenkinsfileContent);
	                    String librariesMsg = libraries.isEmpty() ? "No libraries found" : libraries.toString();

	                    // Get the complexity of the pipeline
	                    String complexityClassification = ComplexityClassifier.classifyJenkinsfile(jenkinsfileContent);
	                    String agentDetails= extractAgentDetails(jenkinsfileContent);

	                    // Get list of external integration
	                    List<String> toolsFirstWords = ExternalIntegrations.extractToolsSectionFirstWords(jenkinsfilePath);
	                    String externalIntegrationsVar = toolsFirstWords.toString();

	                    // Get tools for testing and code quality checks from jenkinsfile with reference to TESTING_TOOLS_LIST
	                    List<String> testingToolsList = Config.TOOLS_LIST;
	                    List<String> foundTools = TestingAndCodeQualityChecks.findToolsInJenkinsfile(jenkinsfileContent, testingToolsList);
	                    String testingAndCodeQualityChecksVar = foundTools.toString();

	                    // Security and Compliance Checks -- SECURITY_TOOLS_LIST in jenkinsfile
	                    List<String> securityToolsList = Config.SECURITY_TOOLS_LIST;
	                    List<String> securityAndComplianceChecksList = SecurityAndComplianceChecks.findSecurityToolsInJenkinsfile(jenkinsfileContent, securityToolsList);

	                    // Cloud Infrastructure and Containerization -- CONTAINERIZATION_LIST in jenkinsfile
	                    List<String> containerizationToolsList = Config.CONTAINERIZATION_LIST;
	                    List<String> cloudInfrastructureAndContainerizationList = CloudInfraAndContainerization.findContainerizationToolsInJenkinsfile(jenkinsfileContent, containerizationToolsList);

	                    // Repository -- from VERSIONING_TOOLS_DICT search with key in jenkinsfile and if found return value
	                    Map<String, String> repositoryToolsDict = Config.VERSIONING_TOOLS_DICT;
	                    List<String> repositoryList = Repository.findRepositoryToolsInJenkinsfile(jenkinsfileContent, repositoryToolsDict);

	                    // Number of Environments/Deployments
	                    int deployStageCount = NumberOfEnvironmentsOrDeployments.countDeployStages(jenkinsfileContent);

	                    // Add all pipeline and external integration data
	                    serviceDetails.put("noOfStages", jobCount);
	                    serviceDetails.put("hasDockerfile", checkForDockerfile(jenkinsFile.getParentFile()));
	                    serviceDetails.put("parallelCount", parallelCount);
	                    serviceDetails.put("branches", getBranches(git));
	                    serviceDetails.put("pipelineType", PipelineScriptComplexity.identifyPipelineType(jenkinsfileContent));
	                    serviceDetails.put("triggersCount", TriggersDetails.extractTriggers(jenkinsfileContent).getCount());
	                    serviceDetails.put("triggers", TriggersDetails.extractTriggers(jenkinsfileContent).getTriggers());
	                    serviceDetails.put("isArtifactoryPresent", artifactoryPresentMsg);
	                    serviceDetails.put("ErrorhandlingAndRetryLogic", tryAndRetryCountMsg);
	                    serviceDetails.put("approvalCount", approvalCount);
	                    serviceDetails.put("libraries", librariesMsg);
	                    serviceDetails.put("complexity", complexityClassification);
	                    serviceDetails.put("externalIntegrationsVar", externalIntegrationsVar);
	                    serviceDetails.put("tools", testingAndCodeQualityChecksVar);
	                    serviceDetails.put("securityAndComplianceChecksVar", securityAndComplianceChecksList.toString());
	                    serviceDetails.put("cloudInfrastructureAndContainerizationVar", cloudInfrastructureAndContainerizationList.toString());
	                    serviceDetails.put("repositoryVar", repositoryList.toString());
	                    serviceDetails.put("variablePassedVar", totalVariablesCount);
	                    serviceDetails.put("configurationParametersOrValuesVar", variablesListStr);
	                    serviceDetails.put("complexityVar", complexityClassification);
	                    serviceDetails.put("agentDetails", agentDetails);

	                    // Add this service details to the map with the service name as the key
	                    serviceParamsMap.put(serviceName, serviceDetails);
	                }
	            } else {
	                repoDetails.put("error", "No Jenkinsfile found in the repository.");
	            }

	            // Convert the serviceParamsMap to JSON format
	            ObjectMapper objectMapper = new ObjectMapper();
	            String json = objectMapper.writeValueAsString(serviceParamsMap);
	            json = json.replace("\\r", "").replace("\\n", "").replace("\\\\", "").replace("\\", "");
	            repoDetails.put("responseDetails", serviceParamsMap);  // Attach service details to the response

	            // Cleanup the cloned repo (optional)
	            deleteDirectory(tempDir);

	        } catch (GitAPIException e) {
	            repoDetails.put("error", e.getMessage());
	        } catch (IOException e) {
	            repoDetails.put("error", "Error reading files: " + e.getMessage());
	        }

	        return repoDetails;
	    }

	    
	    private void printConfigurationFiles(File serviceDir, Map<String, String> configFiles) {
	        // Check and add Dockerfile content
	        File dockerfile = new File(serviceDir, "Dockerfile");
	        if (dockerfile.exists()) {
	            try {
	                String dockerfileContent = new String(Files.readAllBytes(dockerfile.toPath()), StandardCharsets.UTF_8);
	                configFiles.put("Dockerfile", dockerfileContent);
	            } catch (IOException e) {
	                configFiles.put("Dockerfile", "Error reading Dockerfile: " + e.getMessage());
	            }
	        }

	        // Check and add pom.xml content (for Maven-based projects)
	        File pomFile = new File(serviceDir, "pom.xml");
	        if (pomFile.exists()) {
	            try {
	                String pomContent = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);
	                configFiles.put("pom.xml", pomContent);
	            } catch (IOException e) {
	                configFiles.put("pom.xml", "Error reading pom.xml: " + e.getMessage());
	            }
	        }

	        // Check and add build.gradle content (for Gradle-based projects)
	        File gradleFile = new File(serviceDir, "build.gradle");
	        if (gradleFile.exists()) {
	            try {
	                String gradleContent = new String(Files.readAllBytes(gradleFile.toPath()), StandardCharsets.UTF_8);
	                configFiles.put("build.gradle", gradleContent);
	            } catch (IOException e) {
	                configFiles.put("build.gradle", "Error reading build.gradle: " + e.getMessage());
	            }
	        }
	    }

//	    private List<String> getLibraries(String jenkinsfileContent) {
//			// TODO Auto-generated method stub
//			return null;
//		}

//		private String identifyPipelineType(String jenkinsfileContent) {
//			// TODO Auto-generated method stub
//			return null;
//		}

		private List<File> findJenkinsfiles(File rootDir) {
	        List<File> jenkinsfiles = new ArrayList<>();
	        if (rootDir.exists() && rootDir.isDirectory()) {
	            for (File file : rootDir.listFiles()) {
	                if (file.isDirectory()) {
	                    // Recursively check subdirectories
	                    jenkinsfiles.addAll(findJenkinsfiles(file));
	                } else if (file.getName().equals("Jenkinsfile")) {
	                    jenkinsfiles.add(file);
	                }
	            }
	        }
	        return jenkinsfiles;
	    }

	    private String getServiceName(File jenkinsFile) {
	        // Extract service name based on the directory structure
	        // For example, by checking the parent folder
	        return jenkinsFile.getParentFile().getName();
	    }


//	    private int countTriggers(String jenkinsfileContent) {
//	        // Count occurrences of triggers in the Jenkinsfile
//	        return (int) Pattern.compile("triggers\\s?\\{.*?\\}", Pattern.DOTALL).matcher(jenkinsfileContent).results().count();
//	    }

//	    private List<String> extractTriggers(String jenkinsfileContent) {
//	        // Extract trigger definitions from the Jenkinsfile
//	        List<String> triggers = new ArrayList<>();
//	        Matcher matcher = Pattern.compile("triggers\\s?\\{(.*?)\\}", Pattern.DOTALL).matcher(jenkinsfileContent);
//	        while (matcher.find()) {
//	            triggers.add(matcher.group(1).trim());
//	        }
//	        return triggers;
//	    }

	 // Function to extract security and compliance tools from the Jenkinsfile
//	    private List<String> extractSecurityTools(String jenkinsfileContent) {
//	        List<String> securityTools = new ArrayList<>();
//	        
//	        // Check for any security tools in the Jenkinsfile (matching from predefined list SECURITY_TOOLS_LIST)
//	        if (jenkinsfileContent.contains("checkmarx")) securityTools.add("Checkmarx");
//	        if (jenkinsfileContent.contains("sonarQube")) securityTools.add("SonarQube");
//	        if (jenkinsfileContent.contains("fortify")) securityTools.add("Fortify");
//	        // Add other tools as necessary from SECURITY_TOOLS_LIST
//	        
//	        return securityTools;
//	    }

	    // Function to extract cloud infrastructure and containerization details from the Jenkinsfile
//	    private List<String> extractCloudInfrastructure(String jenkinsfileContent) {
//	        List<String> cloudInfrastructure = new ArrayList<>();
//	        List<String> containerizationToolsList = List.of("docker", "kubectl", "openshift");
//	        List<String> foundTools = new ArrayList<>();
//	        for (String tool : containerizationToolsList) {
//	            if (jenkinsfileContent.contains(tool)) {
//	                foundTools.add(tool);
//	            }
//	        }
//	        return foundTools;
//	        
//	    }

	    // Function to extract repository versioning tools from the Jenkinsfile
//	    private List<String> extractRepositories(String jenkinsfileContent) {
//	        List<String> repositories = new ArrayList<>();
//	        
//	        // Check for references to versioning tools such as Git
//	        if (jenkinsfileContent.contains("git")) repositories.add("Git");
//	        if (jenkinsfileContent.contains("svn")) repositories.add("SVN");
//	        if (jenkinsfileContent.contains("maven")) repositories.add("Maven");
//	        if (jenkinsfileContent.contains("gradle")) repositories.add("Gradle");
//	        // Add other versioning tools as necessary from VERSIONING_TOOLS_DICT
//	        
//	        return repositories;
//	    }

	    // Function to count the number of deployment stages (stages containing the word 'deploy')
//	    private int countDeploymentStages(String jenkinsfileContent) {
//	        // Count stages with the keyword 'deploy'
//	        return (int) Pattern.compile("stage\\s?\\{(.*?)deploy(.*?)\\}", Pattern.DOTALL).matcher(jenkinsfileContent).results().count();
//	    }


//	    private int countTryBlocks(String jenkinsfileContent) {
//	        // Count try blocks in the Jenkinsfile
//	        return (int) Pattern.compile("try\\s?\\{").matcher(jenkinsfileContent).results().count();
//	    }

//	    private int countRetryBlocks(String jenkinsfileContent) {
//	        // Count retry blocks in the Jenkinsfile
//	        return (int) Pattern.compile("retry\\s?\\{").matcher(jenkinsfileContent).results().count();
//	    }

//	    private int countApprovalStages(String jenkinsfileContent) {
//	        // Count approval stages (input steps) in the Jenkinsfile
//	        return (int) Pattern.compile("input\\s?\\{").matcher(jenkinsfileContent).results().count();
//	    }

//	    private int countParallelStages(String jenkinsfileContent) {
//	        // Count parallel execution blocks in the Jenkinsfile
//	        return (int) Pattern.compile("parallel\\s?\\{").matcher(jenkinsfileContent).results().count();
//	    }


//	    private String getPipelineComplexity(String jenkinsfileContent) {
//	    	 // Regular expression to match any library declaration
//	        Pattern libraryPattern = Pattern.compile("^\\s*library\\s+\"[^\"]+\"", Pattern.MULTILINE);
//
//	        // Check if the Jenkinsfile is 'Complex'
//	        Matcher libraryMatcher = libraryPattern.matcher(jenkinsfileContent);
//	        if (libraryMatcher.find()) {
//	            return "Complex";
//	        }
//
//	        // Check if the Jenkinsfile is 'Medium'
//	        if (jenkinsfileContent.contains("node {") || jenkinsfileContent.contains("sh") || jenkinsfileContent.contains("bat")) {
//	            return "Medium";
//	        }
//
//	        // Check if the Jenkinsfile is 'Simple'
//	        if (jenkinsfileContent.contains("pipeline {") && !jenkinsfileContent.contains("sh") && !jenkinsfileContent.contains("bat")) {
//	            return "Simple";
//	        }
//
//	        // If none of the above conditions are met, classify as 'Unknown'
//	        return "Unknown";
//	    }

//	    private List<String> extractExternalIntegrations(String jenkinsfileContent) {
//	    	 List<String> lines = new ArrayList<>();
//	    	    
//	    	    // Split the Jenkinsfile content into lines
//	    	    String[] contentLines = jenkinsfileContent.split("\n");
//	    	    
//	    	    // Add each line to the list
//	    	    for (String line : contentLines) {
//	    	        lines.add(line.trim());
//	    	    }
//
//	    	    boolean toolsSection = false;
//	    	    List<String> firstWords = new ArrayList<>(); // Initialize as an empty list
//
//	    	    for (String line : lines) {
//	    	        String strippedLine = line.trim();
//	    	        if (strippedLine.startsWith("tools {")) {
//	    	            toolsSection = true;
//	    	            continue;
//	    	        }
//	    	        if (toolsSection) {
//	    	            if (strippedLine.equals("}")) {
//	    	                break;
//	    	            }
//	    	            String[] words = strippedLine.split("\\s+");
//	    	            if (words.length > 0) {
//	    	                firstWords.add(words[0]);
//	    	            }
//	    	        }
//	    	    }
//
//	    	    return firstWords; //
//	    }

//	    private List<String> extractTestingTools(String jenkinsfileContent) {
//	        // Check for testing tools from a predefined list
//	        List<String> tools = new ArrayList<>();
//	        if (jenkinsfileContent.contains("sh 'mvn test'")) tools.add("Maven");
//	        return tools;
//	    }



	
	   
	    // Helper method to extract agent details from the Jenkinsfile
	    private String extractAgentDetails(String jenkinsfileContent) {
	        String regex = "agent\\s*\\{([\\s\\S]*?)\\}";
	        Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(jenkinsfileContent);

	        if (matcher.find()) {
	            return matcher.group(1).trim();
	        }
	        return "Unknown";
	    }
//	    public static int countStagesInJenkinsfile(String jenkinsfileContent) {
//	        int stagesCount = 0;
//	        int index = 0;
//	        while ((index = jenkinsfileContent.indexOf("stage(", index)) != -1) {
//	            stagesCount++;
//	            index += "stage(".length();
//	        }
//	        return stagesCount;
//	    }
	    private boolean checkForDockerfile(File repoDir) {
	        File dockerfile = new File(repoDir, "Dockerfile");
	        return dockerfile.exists();
	    }
	    
	   
	    private List<String> getBranches(Git git) {
	        List<String> branches = new ArrayList<>();
	        try {
	            // Get the list of branches in the repository
	            Iterable<org.eclipse.jgit.lib.Ref> branchRefs = git.branchList().call();
	            for (org.eclipse.jgit.lib.Ref branchRef : branchRefs) {
	                // Add each branch name to the list
	                branches.add(branchRef.getName());
	            }
	        } catch (GitAPIException e) {
	            System.out.println("Error retrieving branches: " + e.getMessage());
	        }
	        return branches;
	    }

	    private void deleteDirectory(File directory) {
	        if (directory.isDirectory()) {
	            // Get all the files and subdirectories in the directory
	            String[] files = directory.list();
	            if (files != null) {
	                for (String file : files) {
	                    // Recursively delete each file or subdirectory
	                    deleteDirectory(new File(directory, file));
	                }
	            }
	        }
	        // Delete the directory or file itself
	        directory.delete();
	    }
	    
	   
}
