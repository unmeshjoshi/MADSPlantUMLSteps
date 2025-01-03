package com.pumlsteps;

import java.io.File;

public class StepImageGenerator {
    private final String plantUmlJarPath;

    public StepImageGenerator(String plantUmlJarPath) {
        this.plantUmlJarPath = plantUmlJarPath;
    }

    public void generateDiagram(File pumlFile, File outputDir) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                "java", "-jar", plantUmlJarPath, "-o", outputDir.getAbsolutePath(), pumlFile.getAbsolutePath()
            );
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to generate diagram for: " + pumlFile.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating diagram: " + e.getMessage(), e);
        }
    }
}
