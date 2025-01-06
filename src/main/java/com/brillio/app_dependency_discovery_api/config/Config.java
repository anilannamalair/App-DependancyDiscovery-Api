package com.brillio.app_dependency_discovery_api.config;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Config {
    public static final String LOCAL_DIR = "C:\\Users\\Anil.Kumar4\\eclipse-workspace\\application-dependency-discovery2";
    public static final String REPO_DIRECTORY = "C:\\Users\\Anil.Kumar4\\eclipse-workspace\\application-dependency-discovery2\\repo_directory.csv";
    public static final String GENERIC_CONSOLIDATED_ASSESSMENT_REPORT = "C:\\Users\\Anil.Kumar4\\eclipse-workspace\\application-dependency-discovery2\\generic_consolidated_assessment_report.xlsx";

    public static final List<String> TOOLS_LIST = Arrays.asList(
        "maven", "docker", "gradle", "junit", "selenium", "wddriver", "jmeter", "anisble"
    );

    public static final List<String> SECURITY_TOOLS_LIST = Arrays.asList(
        "SonarQube", "Checkmarx", "Veracode", "OWASP ZAP", "Burp Suite", "Acunetix", "Aqua Security", "Twistlock", "Clair", "Snyk", "WhiteSource", "Black Duck", "HashiCorp Vault"
    );

    public static final List<String> CONTAINERIZATION_LIST = Arrays.asList(
        "kubectl", "docker", "openshift", "tanzu", "hdbsql", "hana", "vca", "ey-core", "plural", "aws eks", "az aks", "rancher", "gcloud"
    );

    public static final Map<String, String> VERSIONING_TOOLS_DICT = Map.of(
        "git", "Git",
        "svn", "Subversion",
        "hj", "Helix Core",
        "bit bucket", "Bitbucket"
    );
}