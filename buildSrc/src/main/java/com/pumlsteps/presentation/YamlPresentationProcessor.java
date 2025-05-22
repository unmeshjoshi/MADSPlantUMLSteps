package com.pumlsteps.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pumlsteps.PptGenerator;
import com.pumlsteps.StepParser;
import com.pumlsteps.PumlFile;
import com.pumlsteps.StepImageGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class YamlPresentationProcessor {
    private final File projectDir;
    private final ObjectMapper yamlMapper;
    private final String plantUmlJarPath;

    public YamlPresentationProcessor(File projectDir) {
        this.projectDir = projectDir;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        
        // Look for PlantUML jar in the project directory
        File plantUmlJar = new File(projectDir, "plantuml-1.2024.8.jar");
        
        // If not found in project directory, try parent directory
        if (!plantUmlJar.exists()) {
            plantUmlJar = new File(projectDir.getParentFile(), "plantuml-1.2024.8.jar");
        }
        
        this.plantUmlJarPath = plantUmlJar.getAbsolutePath();
        System.out.println("YamlPresentationProcessor initialized with projectDir: " + projectDir.getAbsolutePath());
        System.out.println("PlantUML jar path: " + plantUmlJarPath + ", exists: " + plantUmlJar.exists());
        
        if (!plantUmlJar.exists()) {
            System.err.println("WARNING: PlantUML jar not found at: " + plantUmlJarPath);
        }
    }

    public void processYamlToPresentation(File yamlFile, File outputFile) {
        try {
            // Read and parse YAML
            PresentationConfig config = yamlMapper.readValue(yamlFile, PresentationConfig.class);
            
            // Create PPT
            PptGenerator generator = new PptGenerator();

            // Process slides directly if available
            if (config.getSlides() != null && !config.getSlides().isEmpty()) {
                for (SlideConfig slide : config.getSlides()) {
                    handleSlide(slide, generator);
                }
            }
            
            // Process sections if available
            if (config.getSections() != null && !config.getSections().isEmpty()) {
                for (SectionConfig section : config.getSections()) {
                    // Process each slide in the section
                    if (section.getSlides() != null) {
                        for (SlideConfig slide : section.getSlides()) {
                            handleSlide(slide, generator);
                        }
                    }
                }
            }

            // Save the presentation
            generator.save(outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Error processing YAML presentation: " + e.getMessage(), e);
        }
    }

    private void handleSlide(SlideConfig slide, PptGenerator generator) {
        switch (slide.getType().toLowerCase()) {
            case "text":
                createTextSlide(slide, generator);
                break;
            case "diagram":
                createDiagramSlide(slide, generator);
                break;
            default:
                throw new IllegalArgumentException("Unknown slide type: " + slide.getType());
        }
    }

    private void createTextSlide(SlideConfig slide, PptGenerator generator) {
        generator.addTextSlide(slide.getTitle(), slide.getBullets());
    }

    private void createDiagramSlide(SlideConfig slide, PptGenerator generator) {
        if (slide.getDiagramRef() != null) {
            String diagramPath = slide.getDiagramRef();
            
            // First, look for the PUML file in the source directory
            File sourceDir = new File(projectDir, "src/diagrams");
            System.out.println("Looking for PUML file in: " + sourceDir.getAbsolutePath() + " for diagram: " + diagramPath);
            File pumlFile = findPumlFile(sourceDir, diagramPath);
            System.out.println("Found PUML file: " + (pumlFile != null ? pumlFile.getAbsolutePath() : "null"));
            
            if (pumlFile != null && pumlFile.exists()) {
                // Generate PNG images on-the-fly for this PUML file
                try {
                    File tempDir = new File(projectDir, "build/temp-png/" + diagramPath);
                    tempDir.mkdirs();
                    System.out.println("Created temp directory for PNG files: " + tempDir.getAbsolutePath());
                    
                    // Parse the PUML file and generate PNG images
                    StepParser parser = new StepParser();
                    PumlFile parsedPumlFile = new PumlFile(pumlFile);
                    var steps = parser.parse(parsedPumlFile).getSteps();
                    
                    // Generate PNG images
                    StepImageGenerator imageGenerator = new StepImageGenerator(plantUmlJarPath);
                    var generatedSteps = imageGenerator.generateDiagrams(steps, tempDir, StepImageGenerator.ImageFormat.PNG);
                    
                    // Add slides for each step
                    for (int i = 0; i < generatedSteps.size(); i++) {
                        File imageFile = generatedSteps.get(i).getImageFile();
                        System.out.println("Adding slide with image file: " + imageFile.getAbsolutePath());
                        
                        // Verify the image file exists and has content
                        if (!imageFile.exists() || imageFile.length() == 0) {
                            System.err.println("WARNING: Image file does not exist or is empty: " + imageFile.getAbsolutePath());
                            continue; // Skip this slide
                        }
                        
                        generator.addStepSlide("", slide.getTitle(), imageFile);
                    }
                    
                    System.out.println("Generated " + generatedSteps.size() + " PNG steps for " + diagramPath);
                } catch (IOException e) {
                    System.err.println("Error generating PNG images for " + diagramPath + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Fall back to looking for existing PNG files
                File diagramDir = new File(projectDir, diagramPath);
                
                // If the directory doesn't exist, try the traditional path format
                if (!diagramDir.exists()) {
                    diagramDir = new File(projectDir, String.format("%s/", diagramPath));
                }
                
                if (diagramDir.exists() && diagramDir.isDirectory()) {
                    File[] steps = diagramDir.listFiles((dir, name) -> name.endsWith(".png"));
                    
                    if (steps != null && steps.length > 0) {
                        createStepsSlides(steps, slide, generator);
                    }
                    
                    // Always add the base diagram if no steps exist or if specifically looking for it
                    File pngFile = new File(diagramDir, diagramPath + ".png");
                    if (pngFile.exists()) {
                        generator.addStepSlide("", slide.getTitle(), pngFile);
                    }
                } else {
                    System.out.println("Warning: Diagram directory not found: " + diagramDir.getAbsolutePath());
                }
            }
        }
    }
    
    private File findPumlFile(File directory, String diagramPath) {
        System.out.println("Finding PUML file for diagram path: " + diagramPath);
        
        // First try direct match
        File directMatch = new File(directory, diagramPath + ".puml");
        System.out.println("Checking direct match: " + directMatch.getAbsolutePath() + ", exists: " + directMatch.exists());
        if (directMatch.exists()) {
            return directMatch;
        }
        
        // Try to find in subdirectories
        String[] parts = diagramPath.split("/");
        String fileName = parts[parts.length - 1] + ".puml";
        System.out.println("Looking for file: " + fileName + " in directory: " + directory.getAbsolutePath());
        
        // Try to find in specific subdirectories based on diagram path
        for (int i = 0; i < parts.length - 1; i++) {
            StringBuilder path = new StringBuilder();
            for (int j = 0; j <= i; j++) {
                path.append(parts[j]).append("/");
            }
            File subDir = new File(directory, path.toString());
            System.out.println("Checking subdirectory: " + subDir.getAbsolutePath() + ", exists: " + subDir.exists());
            if (subDir.exists()) {
                File found = findFileRecursively(subDir, fileName);
                if (found != null) {
                    return found;
                }
            }
        }
        
        // If not found in specific subdirectories, try a full recursive search
        return findFileRecursively(directory, fileName);
    }
    
    private File findFileRecursively(File directory, String fileName) {
        System.out.println("Searching recursively in: " + directory.getAbsolutePath() + " for file: " + fileName);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Directory does not exist or is not a directory: " + directory.getAbsolutePath());
            return null;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            System.out.println("No files found in directory: " + directory.getAbsolutePath());
            return null;
        }
        
        System.out.println("Found " + files.length + " files/directories in: " + directory.getAbsolutePath());
        for (File file : files) {
            if (file.isFile()) {
                System.out.println("Checking file: " + file.getName() + " against target: " + fileName);
                if (file.getName().equals(fileName)) {
                    System.out.println("Found matching file: " + file.getAbsolutePath());
                    return file;
                }
            } else if (file.isDirectory()) {
                System.out.println("Recursing into directory: " + file.getName());
                File found = findFileRecursively(file, fileName);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }

    private void createStepsSlides(File[] steps, SlideConfig slide, PptGenerator generator) {
        // Sort steps by number suffix
        Arrays.sort(steps, Comparator.comparingInt(step -> {
            String name = step.getName();
            return Integer.parseInt(name.substring(name.lastIndexOf("step") + 4, name.lastIndexOf(".png")));
        }));
        
        // Add a slide for each step
        for (File step : steps) {
            generator.addStepSlide("", slide.getTitle(), step);
        }
    }
}
