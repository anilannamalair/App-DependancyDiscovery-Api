package com.brillio.app_dependency_discovery_api.utilities;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class PomFileProcessor {

    // Method to get the service name from a pom.xml file in the given directory
    public static String getServiceNameFromPom(File directory) throws IOException {
        File pomFile = new File(directory, "pom.xml");  // Assuming pom.xml is in the given directory

        // Check if the pom.xml file exists in the directory
        if (!pomFile.exists()) {
            return null;  // Return null if no pom.xml is found in the directory
        }

        // Read and parse the pom.xml file to extract the service name (artifactId)
        FileReader fileReader = new FileReader(pomFile);
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(fileReader);
            // Typically, the service name is the artifactId in the pom.xml
            return model.getArtifactId();
        } catch (Exception e) {
            e.printStackTrace();
            return null;  // Return null if there's an error reading the pom.xml file
        } finally {
            fileReader.close();
        }
    }

    public static void main(String[] args) throws IOException {
        // Example directory containing the pom.xml file (replace with the actual directory)
        File directory = new File("/path/to/directory");  // Replace with the actual path

        // Get the service name from the pom.xml in the given directory
        String serviceName = getServiceNameFromPom(directory);

        if (serviceName != null) {
            System.out.println("Service Name: " + serviceName);
        } else {
            System.out.println("No pom.xml found or could not extract service name.");
        }
    }
}
