package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class StepImageGenerator {
    private final String plantUmlJarPath;

    public StepImageGenerator(String plantUmlJarPath) {
        this.plantUmlJarPath = plantUmlJarPath;
    }

    public List<GeneratedStep> generateDiagrams(List<Step> steps, File outputDir) throws IOException {
        // First create all GeneratedSteps and write PUML files

        //generate all the step puml files.
        generateStepPumlFiles(steps, outputDir);
        // Then generate all PNGs together
        generateStepPngs(outputDir);

        return steps.stream()
                .map(step -> new GeneratedStep(step, GeneratedStep.imageFile(step, outputDir)))
                .collect(Collectors.toList());

    }

    void generateStepPumlFiles(List<Step> steps, File outputDir) {
        steps.forEach(step -> {
            writePumlFile(step, outputDir);
        });
    }

    private void writePumlFile(Step step, File outputDir) {
        File pumlFile = GeneratedStep.pumlFile(step, outputDir);
        try {
            Files.writeString(pumlFile.toPath(), step.getContent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write PUML file: " + pumlFile, e);
        }
    }

    private void generateStepPngs(File outputDirectory) throws IOException {
        System.out.println("Processing puml files in directory " + outputDirectory);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-DPLANTUML_LIMIT_SIZE=8192",  // Increase maximum diagram size
                "-jar", plantUmlJarPath,
                "-tsvg",    // Generate SVG instead of PNG
                "-SdefaultFontSize=12",  // Set consistent font size
                "-Sdpi=96",              // Set consistent DPI
                "-scale 0.8",            // Set smaller scaling
                outputDirectory.getAbsolutePath()
        );
        Process process = processBuilder.start();

        try {
            // Wait for process to complete and check exit code
            int exitCode = process.waitFor();

            // Capture error output if process fails
            if (exitCode != 0) {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                throw new IOException(
                        String.format("PlantUML generation failed with exit code %d: %s",
                                exitCode, errorOutput)
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new IOException("PNG generation was interrupted", e);
        }
    }
}