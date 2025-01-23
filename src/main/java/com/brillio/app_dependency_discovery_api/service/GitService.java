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
import com.brillio.app_dependency_discovery_api.utilities.GitRepoAnalyzer;
import com.brillio.app_dependency_discovery_api.utilities.JsonToFileStructure;
import com.brillio.app_dependency_discovery_api.utilities.NumberOfEnvironmentsOrDeployments;
import com.brillio.app_dependency_discovery_api.utilities.NumberOfLibraries;
import com.brillio.app_dependency_discovery_api.utilities.NumberOfParallelExecution;
import com.brillio.app_dependency_discovery_api.utilities.PipelineScriptComplexity;
import com.brillio.app_dependency_discovery_api.utilities.PomFileProcessor;
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
	
	//private GitRepoAnalyzer analyzer;
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
	                    // Determine service name based on Jenkinsfile directory
	                    String localDirectory = jenkinsFile.getParentFile().getAbsolutePath();
	                    String jenkinsfilePath = jenkinsFile.getAbsolutePath();
	                   // System.out.println("&&&"+localDirectory);
	                    String serviceName = getServiceName(localDirectory,jenkinsFile,repoUrl); 

	                    // Initialize service details map
	                    Map<String, Object> serviceDetails = new HashMap<>();
	                    serviceDetails.put("serviceName", serviceName);

	                    // Process Jenkinsfile content
	                    String jenkinsfileContent = new String(Files.readAllBytes(jenkinsFile.toPath()), StandardCharsets.UTF_8);

	                    // Process configuration files (Dockerfile, pom.xml, build.gradle, etc.)
	                    File serviceDir = jenkinsFile.getParentFile();
//	                    GitRepoAnalyzer analyzer = new GitRepoAnalyzer();
//	                    List<Map<String, String>> analyzedData = analyzer.analyzeRepo(localDirectory);
	                    
	                    //call git repo analyser
	                    Map<String, Object> repoStructure = getRepoStructure(new File(localDirectory));
	                    JsonToFileStructure jsonToFileStructure = new JsonToFileStructure();
	                    List<String> filePaths = jsonToFileStructure.convertMapToFilePaths(repoStructure);
	                    serviceDetails.put("repoStructure", filePaths);
	                    
	                    // Add serviceDetails to the main map
	                    serviceParamsMap.put(serviceName, serviceDetails);
	                    
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
	                    List<File> artifactFiles = findArtifactFiles(rootDir);
	    	            String appType = detectApplicationType(tempDir);
	    	            serviceDetails.put("applicationType", appType);
	    	            
	    	            String appName= PomFileProcessor.getServiceNameFromPom(rootDir);
	    	            serviceDetails.put("appName", appName);
	    	            
	    	            List<Map<String, Object>> artifactDetailsList = new ArrayList<>();
	    	            for (File artifactFile : artifactFiles) {
	    	                artifactDetailsList.add(extractArtifactDetails(artifactFile));  // Extract artifact details
	    	            }
	    	            
	    	           
	    	            serviceDetails.put("artifacts", artifactDetailsList);  

//	    	            repoDetails.put("artifacts", artifactDetailsList);
	    	            boolean isMarvelPipeline = isMarvelPipelineRunning(rootDir);  // New method to check for Marvel pipeline
	    	            serviceDetails.put("isMarvelPipeline", isMarvelPipeline? "Yes" :"No" );

	                    // Add all pipeline and external integration data
	                    serviceDetails.put("noOfStages", jobCount);
	                    serviceDetails.put("hasDockerfile", checkForDockerfile(jenkinsFile.getParentFile())? "Yes" :"No");
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
	                    //serviceDetails.put("complexityVar", complexityClassification);
	                    serviceDetails.put("agentDetails", agentDetails);
	     
	                  
	                    
//	                    serviceDetails.put("analyzedData", analyzedData);

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
		
		private String getServiceName(String localDirectory,File jenkinsFile, String repoUrl) {
		    // Step 1: Check if jenkinsFile is provided
			
		    if (jenkinsFile != null) {  
		        // Get the parent directory of the Jenkinsfile
		        File parentDir = jenkinsFile.getParentFile();
		     
		        // Check if the parent directory exists and is a valid directory
		     if (localDirectory.contains("root")) {
		    	 return parentDir.getName();
		     }else {
		    	 return extractServiceNameFromRepoUrl(repoUrl);
		     }
		    }

		    // Step 2: If no Jenkinsfile, just fall back to repo URL
		    if (repoUrl != null && !repoUrl.isEmpty()) {
		        return extractServiceNameFromRepoUrl(repoUrl);
		    }

		    // If both jenkinsFile and repoUrl are null, throw an exception
		    throw new IllegalArgumentException("Both Jenkinsfile and Repository URL cannot be null or empty.");
		}

		// Helper method to extract service name from repo URL
		private String extractServiceNameFromRepoUrl(String repoUrl) {
		    if (repoUrl == null || repoUrl.isEmpty()) {
		        throw new IllegalArgumentException("Repository URL cannot be null or empty.");
		    }

		    // Extract the service name (last part after the last "/")
		    String serviceName = repoUrl.substring(repoUrl.lastIndexOf("/") + 1);
		    
		    if (serviceName.isEmpty()) {
		        throw new IllegalArgumentException("Invalid repository URL: no service name found.");
		    }

		    return serviceName;
		}


		    
		    
		






	
	   
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
	    
//	    private List<Map<String, Object>> fetchArtifactDetails(String repoPath) throws IOException {
//	        List<Map<String, Object>> artifactDetailsList = new ArrayList<>();
//	        
//	        // Navigate through the artifacts directory
//	        File artifactsDir = new File(repoPath + "/artifacts");
//	        if (artifactsDir.exists() && artifactsDir.isDirectory()) {
//	            File[] serviceDirs = artifactsDir.listFiles(File::isDirectory);
//	            
//	            if (serviceDirs != null) {
//	                for (File serviceDir : serviceDirs) {
//	                    String serviceName = serviceDir.getName();
//	                    File[] artifactFiles = serviceDir.listFiles((dir, name) -> name.endsWith(".jar") || name.endsWith(".zip"));
//	                    
//	                    if (artifactFiles != null) {
//	                        for (File artifact : artifactFiles) {
//	                            Map<String, Object> artifactDetails = new HashMap<>();
//	                            String artifactName = artifact.getName();
//	                            String artifactPath = artifact.getAbsolutePath();
//	                            
//	                            // Assuming category can be inferred from file extensions or naming conventions
//	                            String category = artifactName.contains(".jar") ? "Jar" : "Zip";
//	                            
//	                            artifactDetails.put("artifactName", artifactName);
//	                            artifactDetails.put("artifactPath", artifactPath);
//	                            artifactDetails.put("category", category);
//	                            artifactDetails.put("artifactLocation", serviceDir.getAbsolutePath());
//	                            
//	                            // Add unit test details
//	                            Map<String, Object> unitTestDetails = fetchUnitTestDetails(serviceDir);
//	                            artifactDetails.put("unitTest", unitTestDetails.get("unitTest"));
//	                            artifactDetails.put("unitTestCommands", unitTestDetails.get("unitTestCommands"));
//	                            
//	                            // Add scan tools details
//	                            Map<String, Object> scanDetails = fetchScanDetails(serviceDir);
//	                            artifactDetails.put("scanTest", scanDetails.get("scanTest"));
//	                            artifactDetails.put("scanTools", scanDetails.get("scanTools"));
//	                            
//	                            artifactDetailsList.add(artifactDetails);
//	                        }
//	                    }
//	                }
//	            }
//	        }
//	        return artifactDetailsList;
//	    }

	    private Map<String, Object> fetchUnitTestDetails(File serviceDir) throws IOException {
	        Map<String, Object> unitTestDetails = new HashMap<>();
	        String unitTestCommand = "";
	        
	        // Check for Maven (pom.xml)
	        File pomXml = new File(serviceDir, "pom.xml");
	        if (pomXml.exists()) {
	            // Search for test dependencies (JUnit or TestNG)
	            unitTestCommand = "mvn test";
	            unitTestDetails.put("unitTest", "JUnit / TestNG (Maven)");
	            unitTestDetails.put("unitTestCommands", unitTestCommand);
	        }
	        
	        // Check for Gradle (build.gradle)
	        File buildGradle = new File(serviceDir, "build.gradle");
	        if (buildGradle.exists()) {
	            // Search for test dependencies (JUnit or TestNG)
	            unitTestCommand = "gradle test";
	            unitTestDetails.put("unitTest", "JUnit / TestNG (Gradle)");
	            unitTestDetails.put("unitTestCommands", unitTestCommand);
	        }
	        
	        return unitTestDetails;
	    }

	    private Map<String, Object> fetchScanDetails(File serviceDir) {
	        Map<String, Object> scanDetails = new HashMap<>();
	        String scanTest = "";
	        String scanTools = "";
	        
	        // Example: Check for a security config or scanning tool integration in the Jenkinsfile or other config files
	        File jenkinsfile = new File(serviceDir, "Jenkinsfile");
	        if (jenkinsfile.exists()) {
	            // Check for security scanning tools or configurations
	            scanTest = "Security Scan Present";
	            scanTools = "SonarQube, OWASP Dependency-Check";  // Example tools
	            
	            scanDetails.put("scanTest", scanTest);
	            scanDetails.put("scanTools", scanTools);
	        }
	        
	        return scanDetails;
	    }
	    
	    private List<File> findArtifactFiles(File dir) {
	        List<File> artifactFiles = new ArrayList<>();

	        // Traverse the directory recursively to find artifact files (jar, zip, etc.)
	        File[] files = dir.listFiles();
	        if (files != null) {
	            for (File file : files) {
	                if (file.isDirectory()) {
	                    artifactFiles.addAll(findArtifactFiles(file));  // Recursively search subdirectories
	                } else if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
	                    artifactFiles.add(file);  // Add artifact file if it matches criteria
	                }
	            }
	        }

	        return artifactFiles;
	    }


	    private static Map<String, Object> extractArtifactDetails(File artifactFile) throws IOException {
	        Map<String, Object> artifactDetails = new HashMap<>();
	        String artifactName = artifactFile.getName();
	        String artifactPath = artifactFile.getAbsolutePath();

	        // Assuming artifact category is determined by file extension
	        String category = artifactName.endsWith(".jar") ? "Jar" : "Zip";

	        // Collect artifact details
	        artifactDetails.put("artifactName", artifactName);
	        artifactDetails.put("artifactPath", artifactPath);
	        artifactDetails.put("category", category);
	        artifactDetails.put("artifactLocation", artifactFile.getParentFile().getAbsolutePath());

	        // Look for unit test files and scan tools in the same directory (if any)
	        File parentDir = artifactFile.getParentFile();

	        return artifactDetails;
	    }
	    // Extract Maven unit test command from pom.xml
	    private String extractMavenUnitTestCommand(File pomXml) throws IOException {
	        List<String> lines = Files.readAllLines(pomXml.toPath());
	        for (String line : lines) {
	            if (line.contains("<goal>test</goal>")) {
	                return "mvn test";
	            }
	        }
	        return "Unknown Maven test command";
	    }

	    // Extract Gradle unit test command from build.gradle
	    private String extractGradleUnitTestCommand(File gradleFile) throws IOException {
	        List<String> lines = Files.readAllLines(gradleFile.toPath());
	        for (String line : lines) {
	            if (line.contains("test")) {
	                return "gradle test";
	            }
	        }
	        return "Unknown Gradle test command";
	    }

	    // Extract Docker unit test command from Dockerfile
	    private String extractDockerUnitTestCommand(File dockerFile) throws IOException {
	        List<String> lines = Files.readAllLines(dockerFile.toPath());
	        for (String line : lines) {
	            if (line.contains("RUN") && line.contains("test")) {
	                return "docker run -it <image>";
	            }
	        }
	        return "Unknown Docker test command";
	    }

	    // Extract scan test tool (e.g., SonarQube) from pom.xml or build.gradle
	    private String extractScanTest(File file) throws IOException {
	        List<String> lines = Files.readAllLines(file.toPath());
	        if (file.getName().equals("pom.xml")) {
	            for (String line : lines) {
	                if (line.contains("<sonar:sonar>") || line.contains("sonar")) {
	                    return "SonarQube";
	                }
	            }
	        } else if (file.getName().equals("build.gradle")) {
	            for (String line : lines) {
	                if (line.contains("sonar")) {
	                    return "SonarQube";
	                }
	            }
	        }
	        return "No scan test configured";
	    }

	    // Extract scan tools (e.g., SonarQube Scanner, Trivy) from pom.xml or build.gradle
	    private String extractScanTools(File file) throws IOException {
	        List<String> lines = Files.readAllLines(file.toPath());
	        if (file.getName().equals("pom.xml")) {
	            for (String line : lines) {
	                if (line.contains("sonar:sonar") || line.contains("sonar-scanner")) {
	                    return "SonarQube Scanner";
	                }
	            }
	        } else if (file.getName().equals("build.gradle")) {
	            for (String line : lines) {
	                if (line.contains("sonar")) {
	                    return "SonarQube Scanner";
	                }
	            }
	        } else if (file.getName().equals("Dockerfile")) {
	            for (String line : lines) {
	                if (line.contains("Trivy")) {
	                    return "Trivy";
	                }
	            }
	        }
	        return "No scan tools configured";
	    }
	    
	    private String detectApplicationType(File repoDir) {
	        File[] files = repoDir.listFiles();
	        boolean hasMultipleSubmodules = false;
	        boolean hasSingleBuildConfig = false;
	        boolean hasMultipleBuildConfigs = false;
	        boolean hasDockerfile = false;

	        if (files != null) {
	            for (File file : files) {
	                if (file.isDirectory()) {
	                    // Check for submodules with independent build configurations (pom.xml, build.gradle)
	                    if (new File(file, "pom.xml").exists() || new File(file, "build.gradle").exists()) {
	                        hasMultipleBuildConfigs = true;
	                    }

	                    // Check if the submodules have Dockerfiles or Kubernetes config files (Microservices can use Docker)
	                    if (new File(file, "Dockerfile").exists() || new File(file, "k8s").exists()) {
	                        hasDockerfile = true;
	                    }
	                } else if (file.getName().equals("pom.xml") || file.getName().equals("build.gradle")) {
	                    hasSingleBuildConfig = true;
	                }
	            }
	        }

	        // Basic heuristic for Monolithic vs Microservices
	        if (hasMultipleBuildConfigs) {
	            return "Microservices";
	        }

	        if (hasSingleBuildConfig && !hasMultipleBuildConfigs) {
	            return "Monolithic";
	        }

	        // Additional checks can be made for Dockerfiles or Kubernetes configurations
	        if (hasDockerfile) {
	            return "Microservices (with Docker)";
	        }

	        return "Unknown";
	    }

	    private boolean isMarvelPipelineRunning(File rootDir) {
	        // 1. Check for environment variable MARVEL_PIPELINE
	        String marvelEnvVar = System.getenv("MARVEL_PIPELINE");
	        if (marvelEnvVar != null && !marvelEnvVar.isEmpty()) {
	            return true; // Marvel pipeline environment variable is set
	        }

	        // 2. Check for the presence of Marvel-specific files
	        if (hasMarvelConfigFiles(rootDir)) {
	            return true; // Found Marvel-specific configuration files (.marvel-pipeline, marvel.yaml, etc.)
	        }

	        // 3. Check for Marvel directory in the root of the project
	        if (hasMarvelDirectory(rootDir)) {
	            return true; // Found marvel/ directory
	        }

	        // 4. Check for Marvel-specific steps in Jenkinsfile or other CI configuration files
	        if (containsMarvelStepsInJenkinsFile(rootDir)) {
	            return true; // Found Marvel-related steps in Jenkinsfile
	        }

	        // If none of the above conditions are met, the application is likely not running in Marvel pipeline
	        return false;
	    }

	    private boolean hasMarvelConfigFiles(File rootDir) {
	        // Check for .marvel-pipeline file
	        File marvelFile = new File(rootDir, ".marvel-pipeline");
	        if (marvelFile.exists()) {
	            return true; // Found .marvel-pipeline file
	        }

	        // Check for marvel.yaml or marvel.json
	        File marvelYamlFile = new File(rootDir, "marvel.yaml");
	        if (marvelYamlFile.exists()) {
	            return true; // Found marvel.yaml file
	        }

	        File marvelJsonFile = new File(rootDir, "marvel.json");
	        if (marvelJsonFile.exists()) {
	            return true; // Found marvel.json file
	        }

	        return false;
	    }

	    private boolean hasMarvelDirectory(File rootDir) {
	        // Check if marvel directory exists
	        File marvelDir = new File(rootDir, "marvel");
	        return marvelDir.exists() && marvelDir.isDirectory();
	    }

	    private boolean containsMarvelStepsInJenkinsFile(File rootDir) {
	        File jenkinsFile = new File(rootDir, "Jenkinsfile");
	        if (jenkinsFile.exists()) {
	            try {
	                String content = new String(Files.readAllBytes(jenkinsFile.toPath()), StandardCharsets.UTF_8);
	                // Check for specific Marvel step in Jenkinsfile
	                return content.contains("marvelPipelineStep");
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	        return false;
	    }

	    private Map<String, Object> getRepoStructure(File rootDir) {
	        Map<String, Object> repoStructure = new HashMap<>();
	        
	        // Start with the root directory and explore all subdirectories
	        exploreDirectoryStructure(rootDir, repoStructure);
	        
	        return repoStructure;
	    }

	    private void exploreDirectoryStructure(File currentDir, Map<String, Object> structure) {
	        // Get all files and directories in the current directory
	        File[] files = currentDir.listFiles();
	        
	        if (files != null) {
	            for (File file : files) {
	                // If it's a directory, we need to recurse further
	                if (file.isDirectory()) {
	                    Map<String, Object> subDirStructure = new HashMap<>();
	                    exploreDirectoryStructure(file, subDirStructure);  // Recursive call for subdirectories
	                    structure.put(file.getName(), subDirStructure);  // Add subdirectory structure
	                } else {
	                    // If it's a file, just add it as a leaf node
	                    structure.put(file.getName(), "file");
	                }
	            }
	        }
	    }

}
