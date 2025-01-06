package com.brillio.app_dependency_discovery_api.utilities;

import java.io.File; 
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Git;

public class AssessmentPerRepo {

	public static void logMessage(String message, String logType) {
		// Dummy log method to demonstrate logging
		System.out.println(logType + ": " + message);
	}

	public static void main(String[] args) {
		logMessage("Assessment Tool started.", "Info");

		// Step 1: Read the `repo_directory.csv` file to get the list of `repo_url` and
		// `repo_token`.
		String csvPath = Config.REPO_DIRECTORY;
		List<Map<String, String>> repoDirectory = readCsv(csvPath);

		// Step 2: Duplicate the `generic_consolidated_assessment_report.xlsx` file and
		// name it `consolidated_assessment_report.xlsx`.
		String genericExcelPath = Config.GENERIC_CONSOLIDATED_ASSESSMENT_REPORT;
		try {
			Files.copy(Paths.get(genericExcelPath), Paths.get("consolidated_assessment_report.xlsx"),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Step 3: For each `repo_url` and `repo_token`, execute the `assessPerRepoFunc`
		// function and update the Excel file.
		List<Map<String, Object>> results = new ArrayList<>();
		String localWorkingDir = Config.LOCAL_DIR;

		for (Map<String, String> row : repoDirectory) {
			String repoUrl = row.get("repo_url").replaceAll(" /$", "");
			String repoToken = row.get("repo_token");

			// Create a dynamic folder name
			String dynamicFolder = repoUrl.substring(repoUrl.lastIndexOf('/') + 1);

			// Combine the local working directory with the dynamic folder
			String dynamicPath = localWorkingDir + File.separator + dynamicFolder;

			// Pass the dynamic path as an additional parameter
			Map<String, Object> result = assessPerRepoFunc(repoUrl, repoToken, dynamicPath);
			results.add(result);
		}

		// Convert results to DataFrame (simulated using a List of Maps)
		List<Map<String, Object>> resultsDf = results;

		// Load the duplicated Excel file and append the results
		try (FileInputStream fis = new FileInputStream("consolidated_assessment_report.xlsx");
				Workbook workbook = new XSSFWorkbook(fis);
				FileOutputStream fos = new FileOutputStream("consolidated_assessment_report.xlsx")) {

			Sheet sheet = workbook.getSheetAt(0);
			int startRow = 1; // Starting row for appending data

			for (Map<String, Object> result : resultsDf) {
				Row row = sheet.createRow(startRow++);

				int cellNum = 0;
				for (Object value : result.values()) {
					Cell cell = row.createCell(cellNum++);
					if (value instanceof String) {
						cell.setCellValue((String) value);
					} else if (value instanceof Integer) {
						cell.setCellValue((Integer) value);
					} else if (value instanceof Boolean) {
						cell.setCellValue((Boolean) value);
					}
				}
			}

			workbook.write(fos);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("The consolidated assessment report has been updated successfully.");
		logMessage("Assessment Tool ended.", "Info");
	}

	public static Map<String, Object> assessPerRepoFunc(String repoUrl, String repoToken, String localWorkingDir) {
		logMessage("Working on repo " + repoUrl, "Info");

		// Fork the repository
		// String forkedRepoUrl = LocalFolderCreator.forkRepo(repoUrl, repoToken);
		if (repoUrl != null) {
			// Clone the forked repository to the specified directory
			//LocalFolderCreator.processRepo(repoUrl, localWorkingDir);

			// Run parameters rules
			String jenkinsFolderPath = localWorkingDir;
			System.out.println("Jenkins Folder path" + localWorkingDir);

			// Find and read the Jenkinsfile
			Triple<String, String, Integer> jenkinsfileResult = JenkinsfileFindAndRead
					.findReadJenkinsfile(jenkinsFolderPath);
			String jenkinsfileContent = jenkinsfileResult.getFirst();
			String jenkinsfilePath = jenkinsfileResult.getSecond();
			int responseCode = jenkinsfileResult.getThird();
			System.out.println("jenkinsfileContent" + jenkinsfileContent);
			System.out.println("jenkinsfilePath" + jenkinsfilePath);
			if (responseCode == 200) {
				// Count the number of stages in Jenkinsfile
				int stagesCount = NumberOfStages.countStagesInJenkinsfile(jenkinsfileContent);

				// Identify the pipeline type
				String pipelineType = PipelineScriptComplexity.identifyPipelineType(jenkinsfileContent);
				System.out.println(pipelineType);

				// Get Triggers count
				TriggersDetails.TriggerResult triggersResult = TriggersDetails.extractTriggers(jenkinsfileContent);

				// Extract the trigger count and list
				int triggersCount = triggersResult.getCount();
				List<String> triggersList = triggersResult.getTriggers();

				// Get environment variables containing lines in a file
				List<String[]> variableContentLinesWithDollar = VariablesFinder
						.findLinesWithDollarSignEnvironmentVariables(localWorkingDir);
				int environmentVariablesLinesFilePath = VariablesFinder.writeOutputToFile(localWorkingDir,
						variableContentLinesWithDollar);
				Map<String, Integer> validVariables = VariablesFinder.findValidVariables(localWorkingDir);
				List<String> variablesList = new ArrayList<>(validVariables.keySet());

				int totalVariablesCount = variablesList.size();
				String variablesListStr = String.join(", ", variablesList);

				// Check if Artifactory configuration is present in the Jenkinsfile.
				// Assuming ArtifactManagement is the class containing
				// checkArtifactoryInJenkinsfile
				ArtifactManagement.ArtifactoryResult artifactoryResult = ArtifactManagement
						.checkArtifactoryInJenkinsfile(jenkinsfileContent);
				boolean isArtifactoryPresent = artifactoryResult.isPresent; // Access the 'isPresent' field
				List<String> artifactoryLines = artifactoryResult.artifactoryLines; // Access the 'artifactoryLines'
																					// field

				String artifactoryPresentMsg = isArtifactoryPresent ? "Yes" : "No";
				// Count number of try blocks and retry blocks from Jenkinsfile
				int[] tryRetryResult = ErrorHandlingAndRetryLogic.countTryAndRetryBlocks(jenkinsfileContent);
				int tryCount = tryRetryResult[0]; // First element of the array (try count)
				int retryCount = tryRetryResult[1]; // Second element of the array (retry count)

				// Create the output message
				String tryAndRetryCountMsg = "Error handling count = " + tryCount + ", Retry count = " + retryCount
						+ ".";

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

				// Get list of external integration
				List<String> toolsFirstWords = ExternalIntegrations.extractToolsSectionFirstWords(jenkinsfilePath);
				String externalIntegrationsVar = toolsFirstWords.toString();

				// Get tools for testing and code quality checks from jenkinsfile with reference
				// to TESTING_TOOLS_LIST
				List<String> testingToolsList = Config.TOOLS_LIST;
				List<String> foundTools = TestingAndCodeQualityChecks.findToolsInJenkinsfile(jenkinsfileContent,
						testingToolsList);
				String testingAndCodeQualityChecksVar = foundTools.toString();

				// Security and Compliance Checks -- SECURITY_TOOLS_LIST in jenkinsfile
				List<String> securityToolsList = Config.SECURITY_TOOLS_LIST;
				List<String> securityAndComplianceChecksList = SecurityAndComplianceChecks
						.findSecurityToolsInJenkinsfile(jenkinsfileContent, securityToolsList);

				// Cloud Infrastructure and Containerization -- CONTAINERIZATION_LIST in
				// jenkinsfile
				List<String> containerizationToolsList = Config.CONTAINERIZATION_LIST;
				List<String> cloudInfrastructureAndContainerizationList = CloudInfraAndContainerization
						.findContainerizationToolsInJenkinsfile(jenkinsfileContent, containerizationToolsList);

				// Repository -- from VERSIONING_TOOLS_DICT search with key in jenkinsfile and
				// if found return value
				Map<String, String> repositoryToolsDict = Config.VERSIONING_TOOLS_DICT;
				List<String> repositoryList = Repository.findRepositoryToolsInJenkinsfile(jenkinsfileContent,
						repositoryToolsDict);

				// Number of Environments/Deployments -- Get the count of stage which has
				// 'deploy' word in it in the Jenkinsfile
				int deployStageCount = NumberOfEnvironmentsOrDeployments.countDeployStages(jenkinsfileContent);

				// Return the collected data
				Map<String, Object> result = new HashMap<>();
				result.put("repo_url", repoUrl);
				result.put("stages_count", stagesCount);
				result.put("parallel_count", parallelCount);
				result.put("triggers_count", triggersCount);
				result.put("number_of_environments", deployStageCount);
				result.put("testing_and_code_quality_checks_var", testingAndCodeQualityChecksVar);
				result.put("external_integrations_var", externalIntegrationsVar);
				result.put("pipeline_type", pipelineType);
				result.put("is_artifactory_present", artifactoryPresentMsg);
				result.put("try_and_retry_count", tryAndRetryCountMsg);
				result.put("security_and_compliance_checks_var", securityAndComplianceChecksList.toString());
				result.put("approval_count", approvalCount);
				result.put("cloud_infrastructure_and_containerization_var",
						cloudInfrastructureAndContainerizationList.toString());
				result.put("repository_var", repositoryList.toString());
				result.put("variables_passed_var", totalVariablesCount);
				result.put("configuration_parameters_or_values_var", variablesListStr);
				result.put("libraries", librariesMsg);
				result.put("complexity_var", complexityClassification);

				return result;
			} else {
				logMessage("Jenkinsfile not found or other error occurred.", "Error");
				return Collections.emptyMap();
			}
		} else {
			logMessage("Forking the repository failed.", "Error");
			return Collections.emptyMap();
		}
	}

	public static List<Map<String, String>> readCsv(String csvPath) {
		// Dummy method to simulate reading a CSV file
		List<Map<String, String>> data = new ArrayList<>();
		Map<String, String> row = new HashMap<>();
		row.put("repo_url", "https://github.com/sample/repo");
		row.put("repo_token", "sample_token");
		data.add(row);
		return data;
	}
}

class Config {
	 public static final String LOCAL_DIR = "C:\\Users\\Anil.Kumar4\\p2j";
	    public static final String REPO_DIRECTORY = "C:\\Users\\Anil.Kumar4\\p2j\\repo_directory.csv";
	    public static final String GENERIC_CONSOLIDATED_ASSESSMENT_REPORT = "C:\\Users\\Anil.Kumar4\\p2j\\generic_consolidated_assessment_report.xlsx";
	    
//	public static final String REPO_DIRECTORY = "path/to/repo_directory.csv";
//	public static final String GENERIC_CONSOLIDATED_ASSESSMENT_REPORT = "path/to/generic_consolidated_assessment_report.xlsx";
//	public static final String LOCAL_DIR = "path/to/local/dir";
	public static final List<String> TOOLS_LIST = Arrays.asList("Tool1", "Tool2");
	public static final List<String> SECURITY_TOOLS_LIST = Arrays.asList("SecurityTool1", "SecurityTool2");
	public static final List<String> CONTAINERIZATION_LIST = Arrays.asList("ContainerTool1", "ContainerTool2");
	public static final Map<String, String> VERSIONING_TOOLS_DICT = new HashMap<>();
}

class LocalFolderCreator {
	public static String forkRepo(String repoUrl, String repoToken) {
		// Dummy method to simulate forking a repository
		return "forked_repo_url";
	}

	public static void processRepo(String forkedRepoUrl, String localWorkingDir) {
		cloneRepo(forkedRepoUrl, localWorkingDir);
	}

	private static void cloneRepo(String forkedRepoUrl, String localWorkingDir) {
		File repoDirectory = new File(localWorkingDir);
		if (repoDirectory.exists() && repoDirectory.isDirectory()) {
			System.out.println("Repository already cloned at: " + localWorkingDir);
		} else {
			try {
				System.out.println("Cloning repository from: " + localWorkingDir);
				org.eclipse.jgit.api.Git.cloneRepository().setURI(forkedRepoUrl).setDirectory(repoDirectory).call();
				System.out.println("Repository cloned successfully into: " + localWorkingDir);
			} catch (GitAPIException e) {
				System.out.println("Failed to clone the repository: " + e.getMessage());
			}
		}
	}

//class JenkinsfileFindAndRead {
//    public static Triple<String, String, Integer> findReadJenkinsfile(String jenkinsFolderPath) {
//        // Dummy method to simulate finding and reading a Jenkinsfile
//        return new Triple<>("jenkinsfile_content", "jenkinsfile_path", 200);
//    }
//}
//
//class NoOfStages {
//    public static int countStagesInJenkinsfile(String jenkinsfileContent) {
//        // Dummy method to simulate counting stages in a Jenkinsfile
//        return 5;
//    }
//}

//class PipelineScriptComplexity {
//    public static String identifyPipelineType(String jenkinsfileContent) {
//        // Dummy method to simulate identifying pipeline type
//        return "Pipeline Type";
//    }
//}

//class TriggersDetails {
//    public static Pair<Integer, List<String>> extractTriggers(String jenkinsfileContent) {
//        // Dummy method to simulate extracting triggers
//        return new Pair<>(3, Arrays.asList("Trigger1", "Trigger2"));
//    }
//}
//
//class VariablesFinder {
//    public static List<String> findLinesWithDollarSignEnvironmentVariables(String folderPath) {
//        // Dummy method to simulate finding lines with dollar sign environment variables
//        return Arrays.asList("Line1", "Line2");
//    }

	public static String writeOutputToFile(String folderPath, List<String> lines) {
		// Dummy method to simulate writing output to a file
		return "output_file_path";
	}

	public static Map<String, Integer> findValidVariables(String folderPath) {
		// Dummy method to simulate finding valid variables
		Map<String, Integer> variables = new HashMap<>();
		variables.put("VAR1", 1);
		variables.put("VAR2", 2);
		return variables;
	}
}

//class ArtifactManagement {
//    public static Pair<Boolean, List<String>> checkArtifactoryInJenkinsfile(String jenkinsfileContent) {
//        // Dummy method to simulate checking Artifactory in Jenkinsfile
//        return new Pair<>(true, Arrays.asList("ArtifactoryLine1", "ArtifactoryLine2"));
//    }
//}

//class ErrorHandlingAndRetryLogic {
//    public static Pair<Integer, Integer> countTryAndRetryBlocks(String jenkinsfileContent) {
//        // Dummy method to simulate counting try and retry blocks
//        return new Pair<>(2, 1);
//    }
//}

//class ApprovalGates {
//    public static int countApprovalStages(String jenkinsfileContent) {
//        // Dummy method to simulate counting approval stages
//        return 1;
//    }
//}

//class NoOfParallelExecution {
//    public static Pair<Integer, Integer> countJobsAndParallelBlocks(String jenkinsfileContent) {
//        // Dummy method to simulate counting jobs and parallel blocks
//        return new Pair<>(3, 2);
//    }
//}
//
//class NoOfLibraries {
//    public static List<String> getLibrariesFromJenkinsfileContent(String jenkinsfileContent) {
//        // Dummy method to simulate getting libraries from Jenkinsfile content
//        return Arrays.asList("Lib1", "Lib2");
//    }
//}
//
//class ComplexityClassifier {
//    public static String classifyJenkinsfile(String jenkinsfileContent) {
//        // Dummy method to simulate classifying Jenkinsfile
//        return "Complexity Classification";
//    }
//}
//
//class ExternalIntegrations {
//    public static List<String> extractToolsSectionFirstWords(String jenkinsfilePath) {
//        // Dummy method to simulate extracting tools section first words
//        return Arrays.asList("Tool1", "Tool2");
//    }
//}
//
//class TestingAndCodeQualityChecks {
//    public static List<String> findToolsInJenkinsfile(String jenkinsfileContent, List<String> toolsList) {
//        // Dummy method to simulate finding tools in Jenkinsfile
//        return Arrays.asList("Tool1", "Tool2");
//    }
//}

//class SecurityAndComplianceChecks {
//    public static List<String> findSecurityToolsInJenkinsfile(String jenkinsfileContent, List<String> securityToolsList) {
//        // Dummy method to simulate finding security tools in Jenkinsfile
//        return Arrays.asList("SecurityTool1", "SecurityTool2");
//    }
//}
//
//class CloudInfraAndContainerization {
//    public static List<String> findContainerizationToolsInJenkinsfile(String jenkinsfileContent, List<String> containerizationToolsList) {
//        // Dummy method to simulate finding containerization tools in Jenkinsfile
//        return Arrays.asList("ContainerTool1", "ContainerTool2");
//    }
//}

//class Repository {
//    public static List<String> findRepositoryToolsInJenkinsfile(String jenkinsfileContent, Map<String, String> repositoryToolsDict) {
//        // Dummy method to simulate finding repository tools in Jenkinsfile
//        return Arrays.asList("RepoTool1", "RepoTool2");
//    }
//}
//
//class NumberOfEnvironmentsOrDeployments {
//    public static int countDeployStages(String jenkinsfileContent) {
//        // Dummy method to simulate counting deploy stages
//        return 2;
//    }
//}

class Triple<F, S, T> {
	private final F first;
	private final S second;
	private final T third;

	public Triple(F first, S second, T third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public T getThird() {
		return third;
	}
}

class Pair<F, S> {
	private final F first;
	private final S second;

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}
}