package com.pumlsteps.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pumlsteps.PptGenerator;
import com.pumlsteps.StepImageGenerator;
import com.pumlsteps.FileSystemUtil;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public class YamlPresentationProcessor {
    private final File projectDir;
    private final ObjectMapper yamlMapper;
    private final String plantUmlJarPath;
    private final FileSystemUtil fileSystemUtil;
    private File centralPngDir;  // Directory with all pre-converted PNG files

    public YamlPresentationProcessor(File projectDir, String plantUmlJarPath) {
        this.projectDir = projectDir;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.plantUmlJarPath = plantUmlJarPath;
        this.fileSystemUtil = new FileSystemUtil();
        System.out.println("YamlPresentationProcessor initialized with projectDir: " + projectDir.getAbsolutePath());
        System.out.println("PlantUML jar path: " + plantUmlJarPath + ", exists: " + new File(plantUmlJarPath).exists());

        if (!new File(plantUmlJarPath).exists()) {
            System.err.println("WARNING: PlantUML jar not found at: " + plantUmlJarPath);
        }
    }

    /**
     * Sets the central directory containing all pre-converted PNG files.
     *
     * @param centralPngDir The directory containing all pre-converted PNG files
     */
    public void setCentralPngDirectory(File centralPngDir) {
        this.centralPngDir = centralPngDir;
        System.out.println("Using central PNG directory: " + centralPngDir.getAbsolutePath());
    }

    public void processYamlToPresentation(File yamlFile, File outputFile) throws IOException {
        try {

            // If we have a central PNG directory, use it instead of creating a temporary one
            if (centralPngDir == null || !centralPngDir.exists()) {
                throw new IllegalStateException("Central PNG directory is not set or does not exist.");
            }
            // Read and parse YAML
            PresentationConfig config = yamlMapper.readValue(yamlFile, PresentationConfig.class);

            // Create PPT
            PptGenerator generator = new PptGenerator();
            generator.setPngDirectory(centralPngDir);
            // Process slides
            processSlides(config, generator);

            // Save the presentation
            generator.save(outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Error processing YAML presentation: " + e.getMessage(), e);
        }
    }

    private void processSlides(PresentationConfig config, PptGenerator generator) {
        // Process sections if available
        if (config.getSections() != null) {
            for (SectionConfig section : config.getSections()) {
                processSectionSlides(section, generator);
            }
        }

        // Process slides directly if available
        if (config.getSlides() != null && !config.getSlides().isEmpty()) {
            for (SlideConfig slide : config.getSlides()) {
                handleSlide(slide, generator);
            }
        }
    }

    private void processSectionSlides(SectionConfig section, PptGenerator generator) {
        // Add section separator slide
        if (section.getTitle() != null && !section.getTitle().trim().isEmpty()) {
            generator.addSectionSeparatorSlide(section.getTitle());
        }
        
        // Process each slide in the section
        if (section.getSlides() != null) {
            for (SlideConfig slide : section.getSlides()) {
                handleSlide(slide, generator);
            }
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

            // If no PNG files found in build/diagrams, check the central PNG directory if set
            File[] pngFiles = null;
            
            // First try to find PNG files directly in the diagram path
            File centralDiagramDir = new File(centralPngDir, diagramPath);
            if (centralDiagramDir.exists() && centralDiagramDir.isDirectory()) {
                pngFiles = centralDiagramDir.listFiles((dir, name) -> name.matches("step\\d+\\.png"));
                
                if (pngFiles != null && pngFiles.length > 0) {
                    System.out.println("Found " + pngFiles.length + " PNG files in central directory: " + centralDiagramDir.getAbsolutePath());
                }
            }
            
            // If not found directly, search recursively in the central PNG directory
            if (pngFiles == null || pngFiles.length == 0) {
                System.out.println("No PNG files found directly in " + centralDiagramDir.getAbsolutePath() + ", searching recursively...");
                
                // Find directories that match the diagram path name
                File[] matchingDirs = findDirectoryRecursively(centralPngDir, diagramPath);
                
                if (matchingDirs != null && matchingDirs.length > 0) {
                    for (File matchingDir : matchingDirs) {
                        File[] foundFiles = matchingDir.listFiles((dir, name) -> name.matches("step\\d+\\.png"));
                        if (foundFiles != null && foundFiles.length > 0) {
                            pngFiles = foundFiles;
                            System.out.println("Found " + pngFiles.length + " PNG files in: " + matchingDir.getAbsolutePath());
                            break;
                        }
                    }
                }
            }

            if (pngFiles != null && pngFiles.length > 0) {
                System.out.println("Using " + pngFiles.length + " PNG files");

                // Sort the PNG files by step number
                Arrays.sort(pngFiles, (f1, f2) -> {
                    String name1 = f1.getName();
                    String name2 = f2.getName();
                    int stepIndex = "step".length();
                    int dotIndex1 = name1.lastIndexOf('.');
                    int dotIndex2 = name2.lastIndexOf('.');
                    return Integer.parseInt(name1.substring(stepIndex, dotIndex1)) - 
                           Integer.parseInt(name2.substring(stepIndex, dotIndex2));
                });

                // Add slides directly using the PNG files
                for (File pngFile : pngFiles) {
                    String stepNumber = pngFile.getName().substring(4, pngFile.getName().lastIndexOf('.'));
                    String slideTitle = slide.getTitle() + " - Step " + stepNumber;

                    try {
                        // Verify the PNG is valid before adding to slide
                        BufferedImage testImage = ImageIO.read(pngFile);
                        if (testImage != null) {
                            generator.addImageSlide(slideTitle, pngFile);
                            System.out.println("Successfully added PNG image to slide: " + slideTitle);
                        } else {
                            System.err.println("WARNING: Invalid PNG file, creating placeholder: " + pngFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR adding PNG to slide: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                }
                return;
            }

            throw new IllegalStateException("No PNG files found for diagram: " + diagramPath);

        } else {
            System.err.println("WARNING: Diagram slide has no diagramRef: " + slide.getTitle());
        }
    }

    /**
     * Recursively searches for directories that match the given diagram path name.
     * 
     * @param rootDir The root directory to start the search from
     * @param diagramPath The diagram path to search for
     * @return Array of matching directories, or empty array if none found
     */
    private File[] findDirectoryRecursively(File rootDir, String diagramPath) {
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return new File[0];
        }
        
        // Get the simple name of the diagram (last part of the path)
        String diagramName = new File(diagramPath).getName();
        List<File> matchingDirs = new ArrayList<>();
        
        // Queue for BFS traversal
        Queue<File> queue = new LinkedList<>();
        queue.add(rootDir);
        
        while (!queue.isEmpty()) {
            File currentDir = queue.poll();
            
            // Check if current directory name matches the diagram name
            if (currentDir.getName().equals(diagramName)) {
                matchingDirs.add(currentDir);
            }
            
            // Add subdirectories to the queue
            File[] subdirs = currentDir.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File subdir : subdirs) {
                    queue.add(subdir);
                }
            }
        }
        
        System.out.println("Found " + matchingDirs.size() + " matching directories for diagram: " + diagramPath);
        return matchingDirs.toArray(new File[0]);
    }

    private void copyStylePumlToTempDir(File tempDir) {
        try {
            // Try multiple possible locations for style.puml
            File[] possibleStyleLocations = {
                    new File(projectDir, "src/main/puml/style.puml"),
                    new File(projectDir, "src/puml/style.puml"),
                    new File(projectDir, "puml/style.puml"),
                    new File(projectDir, "style.puml"),
                    new File(projectDir, "build/diagrams/style.puml")
            };

            // Find a valid style.puml file
            File sourceStyleFile = null;
            for (File possibleFile : possibleStyleLocations) {
                if (possibleFile.exists()) {
                    sourceStyleFile = possibleFile;
                    break;
                }
            }

            // Create a basic style.puml content if no file exists
            String styleContent;
            if (sourceStyleFile != null) {
                styleContent = new String(Files.readAllBytes(sourceStyleFile.toPath()));
                System.out.println("Using existing style.puml from: " + sourceStyleFile.getAbsolutePath());
            } else {
                // Create a basic style.puml content
                styleContent = "' Basic style.puml created automatically\n" +
                        "!define FONT_SIZE 14\n" +
                        "!define ARROW_COLOR Black\n" +
                        "!define COMPONENT_BKCOLOR LightSkyBlue\n" +
                        "!define COMPONENT_BORDER_COLOR Black\n" +
                        "skinparam backgroundColor White\n" +
                        "skinparam defaultFontName Arial\n" +
                        "skinparam defaultFontSize 14\n" +
                        "skinparam dpi 300\n" +
                        "skinparam shadowing false\n" +
                        "skinparam roundCorner 10\n" +
                        "skinparam ArrowColor Black\n" +
                        "skinparam ArrowThickness 1.5\n";
                System.out.println("Created default style.puml content");
            }

            // Copy style.puml to the main directory
            File destStyleFile = new File(tempDir, "style.puml");
            try (FileWriter writer = new FileWriter(destStyleFile)) {
                writer.write(styleContent);
            }
            System.out.println("Copied style.puml to: " + destStyleFile.getAbsolutePath());

            // Recursively copy style.puml to all subdirectories
            copyStylePumlToSubdirectories(tempDir, styleContent);

            // Also ensure style.puml is in the target directory and all its subdirectories
            copyStylePumlToSubdirectories(tempDir, styleContent);

        } catch (IOException e) {
            System.err.println("Error handling style.puml: " + e.getMessage());
        }
    }

    /**
     * Recursively copies style.puml content to all subdirectories.
     *
     * @param directory    The directory to process
     * @param styleContent The content of style.puml
     */
    private void copyStylePumlToSubdirectories(File directory, String styleContent) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        // Write style.puml to this directory
        File styleFile = new File(directory, "style.puml");
        if (!styleFile.exists()) {
            try (FileWriter writer = new FileWriter(styleFile)) {
                writer.write(styleContent);
                System.out.println("Created style.puml in: " + styleFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error writing style.puml to " + styleFile.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // Process all subdirectories
        File[] subdirs = directory.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                copyStylePumlToSubdirectories(subdir, styleContent);
            }
        }
    }

    private void createStepsSlides(File[] steps, SlideConfig slide, PptGenerator generator, File tempPngDir) {
        // Sort steps by number suffix
        Arrays.sort(steps, Comparator.comparingInt(step -> {
            String name = step.getName();
            int stepIndex = name.lastIndexOf("step") + 4;
            int dotIndex = name.lastIndexOf('.');
            return Integer.parseInt(name.substring(stepIndex, dotIndex));
        }));

        // If we have a temporary directory for PNGs, use batch processing
        if (tempPngDir != null && tempPngDir.exists()) {
            List<File> svgFilesList = Arrays.asList(steps);
            List<File> pngFilesList = new ArrayList<>();

            // Find corresponding PNG files
            for (File svgFile : svgFilesList) {
                // Get the relative path structure
                String relativePath = "";
                File parentDir = svgFile.getParentFile();
                if (parentDir != null) {
                    String parentPath = parentDir.getAbsolutePath();
                    // Extract the last directory name from the parent path
                    int lastSlashIndex = parentPath.lastIndexOf(File.separator);
                    if (lastSlashIndex >= 0 && lastSlashIndex < parentPath.length() - 1) {
                        relativePath = parentPath.substring(lastSlashIndex + 1) + File.separator;
                    }
                }

                String pngFileName = svgFile.getName().substring(0, svgFile.getName().lastIndexOf('.')) + ".png";

                // First check if the PNG exists in a subdirectory with the same structure
                File subDir = new File(tempPngDir, relativePath);
                File pngFile = new File(subDir, pngFileName);

                // If not found in the subdirectory, check directly in the temp PNG directory
                if (!pngFile.exists()) {
                    pngFile = new File(tempPngDir, pngFileName);
                }

                if (pngFile.exists()) {
                    pngFilesList.add(pngFile);
                    System.out.println("Found pre-converted PNG file: " + pngFile.getAbsolutePath());
                } else {
                    System.err.println("WARNING: Pre-converted PNG file not found for " + svgFile.getName());
                    System.err.println("Looked in: " + pngFile.getAbsolutePath() + " and " + new File(tempPngDir, pngFileName).getAbsolutePath());
                    // Add a placeholder to maintain the same size as svgFilesList
                    pngFilesList.add(null);
                }
            }

            // Add all slides in batch if we have PNG files
            if (!pngFilesList.isEmpty()) {
                // Filter out null PNG files
                List<File> validSvgFiles = new ArrayList<>();
                List<File> validPngFiles = new ArrayList<>();

                for (int i = 0; i < svgFilesList.size(); i++) {
                    if (pngFilesList.get(i) != null) {
                        validSvgFiles.add(svgFilesList.get(i));
                        validPngFiles.add(pngFilesList.get(i));
                    }
                }

                if (!validSvgFiles.isEmpty()) {
                    System.out.println("Adding " + validSvgFiles.size() + " slides in batch with pre-converted PNG files");
                    generator.addStepSlidesInBatch(slide.getTitle(), validSvgFiles, validPngFiles);
                    return;
                }
            }
        }

        // Fall back to individual processing if batch processing is not possible
        System.out.println("WARNING: No pre-converted PNG files found, falling back to individual processing");
        for (File step : steps) {
            generator.addStepSlide("", slide.getTitle(), step);
        }
    }

    private void generateAllPngFilesFromPuml(File tempPngDir) {
        // Find all PUML files in the build/diagrams directory
        File buildDiagramsDir = new File(projectDir, "build/diagrams");
        if (!buildDiagramsDir.exists() || !buildDiagramsDir.isDirectory()) {
            System.err.println("Build diagrams directory not found: " + buildDiagramsDir.getAbsolutePath());
            return;
        }

        System.out.println("Looking for all PUML files in: " + buildDiagramsDir.getAbsolutePath());

        // Collect all PUML files from all subdirectories
        List<File> allPumlFiles = new ArrayList<>();
        collectPumlFiles(buildDiagramsDir, allPumlFiles);

        if (allPumlFiles.isEmpty()) {
            System.err.println("No PUML files found in " + buildDiagramsDir.getAbsolutePath());
            return;
        }

        System.out.println("Found " + allPumlFiles.size() + " PUML files in total");

        // Generate PNG files directly using PlantUML
        try {
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-DPLANTUML_LIMIT_SIZE=8192");  // Increase maximum diagram size
            command.add("-jar");
            command.add(plantUmlJarPath);

            // PNG-specific options
            command.add("-tpng");                  // Generate PNG files
            command.add("-SdefaultFontSize=12");   // Set consistent font size
            command.add("-Sdpi=300");              // Higher DPI for better quality
            command.add("-scale 1.0");             // Full scale for better visibility

            // Specify output directory
            command.add("-o");
            command.add(tempPngDir.getAbsolutePath());

            // Add all PUML files to the command
            for (File pumlFile : allPumlFiles) {
                command.add(pumlFile.getAbsolutePath());
            }

            System.out.println("Running PlantUML to generate PNG files for all " + allPumlFiles.size() + " PUML files...");
            System.out.println("Command: " + String.join(" ", command));

            // Run the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture and log the output
            StringBuilder outputLog = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLog.append(line).append("\n");
                    if (line.contains("ERROR") || line.contains("WARN") || line.contains("Exception")) {
                        System.err.println("PlantUML PUML to PNG: " + line);
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("PlantUML conversion exited with code " + exitCode);
                System.err.println("Output log: " + outputLog.toString());
            } else {
                System.out.println("PlantUML conversion completed successfully for all " + allPumlFiles.size() + " PUML files");
            }

            // Verify all PNG files were created
            int validCount = 0;
            int invalidCount = 0;

            for (File pumlFile : allPumlFiles) {
                String pngFileName = pumlFile.getName().substring(0, pumlFile.getName().lastIndexOf('.')) + ".png";
                File pngFile = new File(tempPngDir, pngFileName);

                // Verify the PNG file was created and is valid
                if (!pngFile.exists() || pngFile.length() == 0) {
                    System.err.println("WARNING: PNG file not created or is empty for " + pumlFile.getAbsolutePath());
                    StepImageGenerator.createPlaceholderPng(pngFile, pumlFile.getName());
                    invalidCount++;
                } else {
                    try {
                        // Verify the PNG is valid by trying to read it
                        BufferedImage testImage = ImageIO.read(pngFile);
                        if (testImage == null) {
                            System.err.println("WARNING: Created PNG file is not a valid image: " + pngFile.getAbsolutePath());
                            StepImageGenerator.createPlaceholderPng(pngFile, pumlFile.getName());
                            invalidCount++;
                        } else {
                            validCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("WARNING: Error validating PNG file: " + e.getMessage());
                        StepImageGenerator.createPlaceholderPng(pngFile, pumlFile.getName());
                        invalidCount++;
                    }
                }
            }

            System.out.println("Successfully processed " + validCount + " PUML files to valid PNG files");
            if (invalidCount > 0) {
                System.err.println("WARNING: " + invalidCount + " PUML files failed to generate valid PNG files");
            }
        } catch (Exception e) {
            System.err.println("Error in batch conversion of PUML files to PNG: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates PNG files directly from PUML files for a specific diagram path.
     *
     * @param diagramPath The path to the diagram directory
     * @param tempPngDir  The temporary directory to store PNG files
     * @return Array of generated PNG files
     */
    private File[] generatePngFilesFromPuml(String diagramPath, File tempPngDir) {
        // Look for PUML files in the build/diagrams directory instead of src/main/puml
        File pumlDir = new File(projectDir, "build/diagrams/" + diagramPath);
        if (!pumlDir.exists() || !pumlDir.isDirectory()) {
            System.err.println("Diagram directory not found: " + pumlDir.getAbsolutePath());
            return new File[0];
        }

        System.out.println("Looking for PUML files in: " + pumlDir.getAbsolutePath());

        // Find all PUML files in the directory
        File[] pumlFiles = pumlDir.listFiles((dir, name) -> name.endsWith(".puml"));

        // If no PUML files found directly, search subdirectories
        if (pumlFiles == null || pumlFiles.length == 0) {
            List<File> allPumlFiles = new ArrayList<>();
            collectPumlFiles(pumlDir, allPumlFiles);

            if (allPumlFiles.isEmpty()) {
                System.err.println("No PUML step files found in " + pumlDir.getAbsolutePath() + " or its subdirectories");
                return new File[0];
            }

            pumlFiles = allPumlFiles.toArray(new File[0]);
        }

        // Copy style.puml to the PUML directory and all subdirectories to ensure consistent styling
        copyStylePumlToTempDir(tempPngDir);

        System.out.println("Generating PNG files for " + pumlFiles.length + " PUML files...");

        // Generate PNG files directly using PlantUML
        try {
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-DPLANTUML_LIMIT_SIZE=8192");  // Increase maximum diagram size
            command.add("-jar");
            command.add(plantUmlJarPath);

            // PNG-specific options
            command.add("-tpng");                  // Generate PNG files
            command.add("-SdefaultFontSize=12");   // Set consistent font size
            command.add("-Sdpi=300");              // Higher DPI for better quality
            command.add("-scale 1.0");             // Full scale for better visibility

            // Specify output directory
            command.add("-o");
            command.add(tempPngDir.getAbsolutePath());

            // Add all PUML files to the command
            for (File pumlFile : pumlFiles) {
                command.add(pumlFile.getAbsolutePath());
            }

            System.out.println("Running PlantUML to generate PNG files...");

            // Run the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture and log the output
            StringBuilder outputLog = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLog.append(line).append("\n");
                    if (line.contains("ERROR") || line.contains("WARN") || line.contains("Exception")) {
                        System.err.println("PlantUML: " + line);
                    }
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("PlantUML exited with code " + exitCode);
                System.err.println("Output log: " + outputLog.toString());
            } else {
                System.out.println("PlantUML completed successfully");
            }

            // Collect and return all generated PNG files
            List<File> pngFiles = new ArrayList<>();
            for (File pumlFile : pumlFiles) {
                String baseName = pumlFile.getName().substring(0, pumlFile.getName().lastIndexOf('.'));
                File pngFile = new File(tempPngDir, baseName + ".png");
                if (pngFile.exists() && pngFile.length() > 0) {
                    pngFiles.add(pngFile);
                } else {
                    System.err.println("WARNING: PNG file not created for " + pumlFile.getName());
                    // Create a placeholder PNG
                    StepImageGenerator.createPlaceholderPng(pngFile, pumlFile.getName());
                    pngFiles.add(pngFile);
                }
            }

            System.out.println("Generated " + pngFiles.size() + " PNG files");
            return pngFiles.toArray(new File[0]);

        } catch (Exception e) {
            System.err.println("Error generating PNG files: " + e.getMessage());
            e.printStackTrace();
            return new File[0];
        }
    }

    /**
     * Recursively collects all PUML files from a directory and its subdirectories.
     *
     * @param directory The directory to search
     * @param pumlFiles The list to add found PUML files to
     */
    private void collectPumlFiles(File directory, List<File> pumlFiles) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                collectPumlFiles(file, pumlFiles);
            } else if (file.getName().endsWith(".puml") && !file.getName().equals("style.puml")) {
                pumlFiles.add(file);
            }
        }
    }

    /**
     * Recursively collects all step PUML files from a directory and its subdirectories.
     *
     * @param directory The directory to search
     * @param pumlFiles The list to add found PUML files to
     */
    private void collectStepPumlFiles(File directory, List<File> pumlFiles) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                collectStepPumlFiles(file, pumlFiles);
            } else if (file.getName().matches("step\\d+\\.puml")) {
                pumlFiles.add(file);
            }
        }
    }

    /**
     * Recursively collects all PNG files from a directory and its subdirectories.
     *
     * @param directory The directory to search
     * @param pngFiles  The list to add found PNG files to
     */
    private void collectPngFiles(File directory, List<File> pngFiles) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                collectPngFiles(file, pngFiles);
            } else if (file.getName().endsWith(".png") && file.getName().matches("step\\d+\\.png")) {
                pngFiles.add(file);
            }
        }
    }
}
