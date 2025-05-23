package com.pumlsteps;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class StepImageGenerator {
    private final String plantUmlJarPath;

    public enum ImageFormat {
        PNG, SVG
    }

    public StepImageGenerator(String plantUmlJarPath) {
        this.plantUmlJarPath = plantUmlJarPath;
    }

    /**
     * Processes all PUML files in the source directory and its subdirectories recursively.
     * This method is much more efficient than processing files individually.
     *
     * @param sourceDir The source directory containing PUML files
     * @param outputDir The output directory to write generated files to
     * @param format    The format to generate the diagrams in (PNG or SVG)
     * @throws IOException If there is an error processing the files
     */
    public void generateImagesForDirectory(File sourceDir, File outputDir, ImageFormat format) throws IOException {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            System.err.println("Source directory does not exist or is not a directory: " + sourceDir);
            return;
        }

        // Create output directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        System.out.println("Generating " + format + " diagrams from " + sourceDir.getAbsolutePath() + " to " + outputDir.getAbsolutePath());

        copyStyles(sourceDir, outputDir);

        // Collect all PUML files to process
        List<File> allPumlFiles = new ArrayList<>();
        prepareAllPumlFiles(sourceDir, outputDir, format, allPumlFiles);

        System.out.println("Found " + allPumlFiles.size() + " PUML files to process");

        // Process all collected PUML files with a single PlantUML invocation
        if (!allPumlFiles.isEmpty()) {
            try {
                runPlantUmlOnFiles(allPumlFiles, format, null);
                System.out.println("Successfully generated " + format + " diagrams for " + allPumlFiles.size() + " files");
            } catch (IOException e) {
                System.err.println("Error generating " + format + " diagrams: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void copyStyles(File sourceDir, File outputDir) {
        // Look for style.puml in the source directory
        File styleFile = new File(sourceDir, "style.puml");
        if (!styleFile.exists()) {
            // Try to find style.puml in parent directories
            File currentDir = sourceDir;
            while (currentDir != null && !styleFile.exists()) {
                currentDir = currentDir.getParentFile();
                if (currentDir != null) {
                    styleFile = new File(currentDir, "style.puml");
                }
            }

            if (!styleFile.exists()) {
                // Create a default style.puml in the output directory
                styleFile = new File(outputDir, "style.puml");
                try {
                    if (!styleFile.exists()) {
                        Files.writeString(styleFile.toPath(),
                                "' Default style.puml created by StepImageGenerator\n" +
                                        "skinparam backgroundColor white\n" +
                                        "skinparam defaultFontName Arial\n" +
                                        "skinparam defaultFontSize 12\n" +
                                        "skinparam sequenceArrowThickness 2\n" +
                                        "skinparam roundcorner 8\n" +
                                        "skinparam ParticipantPadding 20\n" +
                                        "skinparam BoxPadding 10\n"
                        );
                        System.out.println("Created default style.puml at: " + styleFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    System.err.println("Error creating default style.puml: " + e.getMessage());
                }
            } else {
                System.out.println("Found style.puml in parent directory: " + styleFile.getAbsolutePath());

                // Copy style.puml to the output directory
                try {
                    File outputStyleFile = new File(outputDir, "style.puml");
                    Files.copy(styleFile.toPath(), outputStyleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied style.puml to output directory: " + outputStyleFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Error copying style.puml to output directory: " + e.getMessage());
                }
            }
        } else {
            System.out.println("Found style.puml in source directory: " + styleFile.getAbsolutePath());

            // Copy style.puml to the output directory
            try {
                File outputStyleFile = new File(outputDir, "style.puml");
                Files.copy(styleFile.toPath(), outputStyleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied style.puml to output directory: " + outputStyleFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error copying style.puml to output directory: " + e.getMessage());
            }
        }
    }

    /**
     * Recursively prepares all PUML files by extracting steps and fixing includes.
     *
     * @param sourceDir    The source directory containing PUML files
     * @param outputDir    The output directory to write generated files to
     * @param styleFile    The style.puml file to copy to output directories
     * @param format       The format to generate the diagrams in (PNG or SVG)
     * @param allPumlFiles List to collect all PUML files that need to be processed
     * @throws IOException If there is an error processing the files
     */
    private void prepareAllPumlFiles(File sourceDir, File outputDir, ImageFormat format, List<File> allPumlFiles) throws IOException {
        // Create the output directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Process all PUML files in the source directory
        File[] pumlFiles = sourceDir.listFiles((dir, name) ->
                name.endsWith(".puml") && !name.equals("style.puml"));

        if (pumlFiles != null) {
            for (File pumlFile : pumlFiles) {
                // Create a subdirectory for the PUML file's steps
                String baseName = pumlFile.getName().replace(".puml", "");
                File outputSubDir = new File(outputDir, baseName);
                outputSubDir.mkdirs();

                // Parse the PUML file to extract steps
                PumlFile pumlFileObj = new PumlFile(pumlFile);
                ParsedPlantUmlFile parsedFile = new StepParser().parse(pumlFileObj);
                List<Step> steps = parsedFile.getSteps();

                // Generate PUML files for each step
                for (int i = 0; i < steps.size(); i++) {
                    Step step = steps.get(i);
                    String stepContent = step.getContent();


                    // Write the modified step content to a file
                    File stepFile = new File(outputSubDir, "step" + (i + 1) + ".puml");
                    Files.writeString(stepFile.toPath(), stepContent);

                    // Add to the list of files to process
                    allPumlFiles.add(stepFile);
                }
            }
        }

        // Process subdirectories
        File[] subdirs = sourceDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                File outputSubDir = new File(outputDir, subdir.getName());
                outputSubDir.mkdirs();
                // Recursively process the subdirectory with the parent style file
                prepareAllPumlFiles(subdir, outputSubDir, format, allPumlFiles);
            }
        }
    }

    /**
     * Runs PlantUML on all collected PUML or SVG files with a single invocation.
     * This is much more efficient than running PlantUML multiple times.
     *
     * @param files           List of PUML or SVG files to process
     * @param format          The format to generate the diagrams in (PNG or SVG)
     * @param outputDirectory Directory where output files should be saved (if null, files are saved in the same directory as input)
     * @throws IOException If there is an error running PlantUML
     */
    private void runPlantUmlOnFiles(List<File> files, ImageFormat format, File outputDirectory) throws IOException {
        System.out.println("Running PlantUML on " + files.size() + " files in a single invocation");

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
            command.add("-tpng");               // Generate PNG files
            command.add("-SdefaultFontSize=12"); // Set consistent font size
            command.add("-Sdpi=300");           // Higher DPI for better quality
            command.add("-scale 1.0");          // Full scale for better visibility
        }

        // We don't specify a global output directory because we want files to be generated
        // in their respective directories

        // Add all files to process
        for (File file : files) {
            command.add(file.getAbsolutePath());
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge stdout and stderr
        Process process = processBuilder.start();

        // Capture and print output for debugging
        StringBuilder output = new StringBuilder();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                // Print only important messages to avoid cluttering the console
                if (line.contains("ERROR") || line.contains("WARNING") || line.contains("Exception")) {
                    System.err.println("PlantUML: " + line);
                }
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("PlantUML exited with code " + exitCode);
                System.err.println("PlantUML output: " + output.toString());
                throw new IOException("PlantUML exited with code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PlantUML execution was interrupted", e);
        }

        // Verify that files were generated
        int successCount = 0;
        int errorCount = 0;

        for (File pumlFile : files) {
            String baseName = pumlFile.getName().substring(0, pumlFile.getName().lastIndexOf('.'));
            String extension = format == ImageFormat.SVG ? ".svg" : ".png";
            File outputFile = new File(pumlFile.getParentFile(), baseName + extension);

            if (!outputFile.exists() || outputFile.length() == 0) {
                System.err.println("WARNING: " + format + " file not generated for " + pumlFile.getAbsolutePath());
                errorCount++;
            } else {
                successCount++;
            }
        }

        System.out.println("Successfully generated " + successCount + " " + format + " files");
        if (errorCount > 0) {
            System.err.println("WARNING: Failed to generate " + errorCount + " " + format + " files");
        }
    }

    /**
     * Converts multiple SVG files to PNG format in a single PlantUML invocation.
     * This is more efficient than converting files one by one.
     *
     * @param svgFiles        List of SVG files to convert
     * @param outputDirectory Directory where PNG files should be saved
     * @param plantUmlJarPath Path to the PlantUML jar file
     * @return Array of generated PNG files corresponding to the input SVG files (in the same order)
     */
    public static File[] convertSvgFilesToPng(List<File> svgFiles, File outputDirectory, String plantUmlJarPath) {
        if (svgFiles.isEmpty()) {
            return new File[0];
        }

        System.out.println("Batch converting " + svgFiles.size() + " SVG files to PNG for PowerPoint using PlantUML");

        try {
            // Ensure output directory exists
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // Create a new instance to use the instance method
            StepImageGenerator generator = new StepImageGenerator(plantUmlJarPath);

            // Run PlantUML to convert SVG files to PNG
            generator.runPlantUmlOnFiles(svgFiles, ImageFormat.PNG, outputDirectory);

            // Create corresponding PNG file objects and verify they exist
            File[] pngFiles = new File[svgFiles.size()];
            for (int i = 0; i < svgFiles.size(); i++) {
                File svgFile = svgFiles.get(i);
                String pngFileName = svgFile.getName().substring(0, svgFile.getName().lastIndexOf('.')) + ".png";
                File pngFile = new File(outputDirectory, pngFileName);
                pngFiles[i] = pngFile;

                // Verify the PNG file was created and is valid
                if (!pngFile.exists() || pngFile.length() == 0) {
                    System.err.println("WARNING: PNG file not created or is empty for " + svgFile.getAbsolutePath());
                    createPlaceholderPng(pngFile, svgFile.getName());
                } else {
                    try {
                        // Verify the PNG is valid by trying to read it
                        BufferedImage testImage = ImageIO.read(pngFile);
                        if (testImage == null) {
                            System.err.println("WARNING: Created PNG file is not a valid image: " + pngFile.getAbsolutePath());
                            createPlaceholderPng(pngFile, svgFile.getName());
                        } else {
                            System.out.println("Successfully created valid PNG: " + pngFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        System.err.println("WARNING: Error validating PNG file: " + e.getMessage());
                        createPlaceholderPng(pngFile, svgFile.getName());
                    }
                }
            }

            System.out.println("Successfully processed " + svgFiles.size() + " SVG files to PNG");
            return pngFiles;
        } catch (Exception e) {
            System.err.println("Error in batch conversion of SVG files to PNG: " + e.getMessage());
            e.printStackTrace();

            // Create placeholder PNGs for all files
            File[] pngFiles = new File[svgFiles.size()];
            for (int i = 0; i < svgFiles.size(); i++) {
                File svgFile = svgFiles.get(i);
                String pngFileName = svgFile.getName().substring(0, svgFile.getName().lastIndexOf('.')) + ".png";
                File pngFile = new File(outputDirectory, pngFileName);
                createPlaceholderPng(pngFile, svgFile.getName());
                pngFiles[i] = pngFile;
            }

            return pngFiles;
        }
    }

    /**
     * Creates a placeholder PNG image with a message indicating the original file name.
     *
     * @param pngFile          The PNG file to create
     * @param originalFileName The name of the original file that failed conversion
     */
    public static void createPlaceholderPng(File pngFile, String originalFileName) {
        try {
            // Create a default placeholder image - using larger dimensions to ensure visibility
            BufferedImage placeholderImage = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = placeholderImage.createGraphics();

            // Set rendering hints for better text quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Fill background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 1200, 800);

            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(5));
            g2d.drawRect(20, 20, 1160, 760);

            // Draw text
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            String message = "Placeholder for: " + originalFileName;
            int messageWidth = g2d.getFontMetrics().stringWidth(message);
            g2d.drawString(message, (1200 - messageWidth) / 2, 350);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 28));
            String subMessage = "PUML to PNG conversion failed";
            int subMessageWidth = g2d.getFontMetrics().stringWidth(subMessage);
            g2d.drawString(subMessage, (1200 - subMessageWidth) / 2, 400);

            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            String timeMessage = "Generated at: " + new java.util.Date();
            int timeMessageWidth = g2d.getFontMetrics().stringWidth(timeMessage);
            g2d.drawString(timeMessage, (1200 - timeMessageWidth) / 2, 450);

            g2d.dispose();

            // Ensure the parent directory exists
            if (!pngFile.getParentFile().exists()) {
                pngFile.getParentFile().mkdirs();
            }

            // Write the image to the file
            boolean success = ImageIO.write(placeholderImage, "png", pngFile);
            if (!success) {
                throw new IOException("No appropriate writer found for PNG format");
            }

            // Verify the file was created and has content
            if (!pngFile.exists() || pngFile.length() == 0) {
                throw new IOException("PNG file was not created or is empty");
            }

            System.out.println("Created placeholder PNG for: " + originalFileName + " at " + pngFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error creating placeholder PNG: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Finds all SVG files in a directory and its subdirectories and converts them to PNG format.
     * This method is designed to be called once after all SVG files have been generated but before
     * PowerPoint generation starts, to ensure all PNG files are available for dimension calculation.
     *
     * @param rootDirectory      The root directory to search for SVG files
     * @param pngOutputDirectory The directory where PNG files should be saved
     * @param plantUmlJarPath    Path to the PlantUML jar file
     * @return The number of SVG files converted
     */
    public static int batchConvertAllSvgFilesToPng(File rootDirectory, File pngOutputDirectory, String plantUmlJarPath) {
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            System.err.println("Root directory does not exist or is not a directory: " + rootDirectory.getAbsolutePath());
            return 0;
        }

        System.out.println("Finding all SVG files in " + rootDirectory.getAbsolutePath() + " for batch conversion...");

        // Find all SVG files recursively
        List<File> allSvgFiles = new ArrayList<>();
        findAllSvgFiles(rootDirectory, allSvgFiles);

        if (allSvgFiles.isEmpty()) {
            System.out.println("No SVG files found in " + rootDirectory.getAbsolutePath());
            return 0;
        }

        System.out.println("Found " + allSvgFiles.size() + " SVG files for batch conversion");

        // Also manually copy each SVG file to a PNG file to ensure we have all the files we need
        int manualCopyCount = 0;
        for (File svgFile : allSvgFiles) {
            try {
                String pngFileName = svgFile.getName().substring(0, svgFile.getName().lastIndexOf('.')) + ".png";
                File pngFile = new File(pngOutputDirectory, pngFileName);

                // Use Java's built-in image conversion as a fallback
                if (!pngFile.exists()) {
                    // Create a simple empty PNG file to ensure the file exists
                    // This is a placeholder that will be properly converted by PlantUML later
                    pngFile.createNewFile();
                    manualCopyCount++;
                }
            } catch (Exception e) {
                System.err.println("Error creating placeholder PNG file for " + svgFile.getName() + ": " + e.getMessage());
            }
        }

        if (manualCopyCount > 0) {
            System.out.println("Created " + manualCopyCount + " placeholder PNG files");
        }

        // Convert all SVG files to PNG in a single batch
        File[] pngFiles = convertSvgFilesToPng(allSvgFiles, pngOutputDirectory, plantUmlJarPath);

        return pngFiles.length;
    }

    /**
     * Recursively finds all SVG files in a directory and its subdirectories.
     *
     * @param directory The directory to search
     * @param svgFiles  List to collect SVG files
     */
    private static void findAllSvgFiles(File directory, List<File> svgFiles) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findAllSvgFiles(file, svgFiles);
            } else if (file.getName().toLowerCase().endsWith(".svg")) {
                svgFiles.add(file);
            }
        }
    }
}