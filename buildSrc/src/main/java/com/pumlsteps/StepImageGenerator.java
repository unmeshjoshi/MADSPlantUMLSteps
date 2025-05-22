package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StepImageGenerator {
    private final String plantUmlJarPath;
    
    public enum ImageFormat {
        PNG, SVG
    }

    public StepImageGenerator(String plantUmlJarPath) {
        this.plantUmlJarPath = plantUmlJarPath;
    }

    public List<GeneratedStep> generateDiagrams(List<Step> steps, File outputDir, ImageFormat format) throws IOException {
        // First create all GeneratedSteps and write PUML files

        //generate all the step puml files.
        generateStepPumlFiles(steps, outputDir);
        // Then generate all images together
        generateStepImages(outputDir, format);

        return steps.stream()
                .map(step -> new GeneratedStep(step, GeneratedStep.imageFile(step, outputDir, format)))
                .collect(Collectors.toList());

    }
    
    // For backward compatibility
    public List<GeneratedStep> generateDiagrams(List<Step> steps, File outputDir) throws IOException {
        return generateDiagrams(steps, outputDir, ImageFormat.PNG);
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

    private void generateStepImages(File outputDirectory, ImageFormat format) throws IOException {
        System.out.println("Processing puml files in directory " + outputDirectory);
        
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-DPLANTUML_LIMIT_SIZE=8192");  // Increase maximum diagram size
        command.add("-jar");
        command.add(plantUmlJarPath);
        
        // Add format-specific options
        if (format == ImageFormat.SVG) {
            command.add("-tsvg");    // Generate SVG files
            command.add("-SdefaultFontSize=12");  // Set consistent font size
            command.add("-Sdpi=96");              // Standard DPI for web
            command.add("-scale 0.8");            // Smaller scaling for SVG
        } else {
            // Default is PNG
            command.add("-SdefaultFontSize=12");  // Set consistent font size
            command.add("-Sdpi=300");             // Higher DPI for better quality
            command.add("-scale 1.0");            // Full scale for better visibility
        }
        
        command.add(outputDirectory.getAbsolutePath());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
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