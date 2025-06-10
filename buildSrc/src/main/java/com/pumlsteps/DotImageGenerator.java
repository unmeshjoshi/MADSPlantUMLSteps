package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DotImageGenerator {

    public enum ImageFormat {
        PNG, SVG
    }

    public DotImageGenerator() {
    }

    /**
     * Processes all DOT files in the source directory and its subdirectories recursively.
     *
     * @param sourceDir The source directory containing DOT files
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

        System.out.println("Generating " + format + " diagrams from DOT files in " + sourceDir.getAbsolutePath() + " to " + outputDir.getAbsolutePath());

        // Check if Graphviz is installed
        if (!isGraphvizInstalled()) {
            System.err.println("ERROR: Graphviz is not installed or 'dot' command is not in PATH.");
            System.err.println("Please install Graphviz: https://graphviz.org/download/");
            return;
        }

        // Collect all DOT files to process
        List<File> allDotFiles = new ArrayList<>();
        prepareAllDotFiles(sourceDir, outputDir, format, allDotFiles);

        System.out.println("Found " + allDotFiles.size() + " DOT files to process");

        // Process all collected DOT files
        if (!allDotFiles.isEmpty()) {
            try {
                processDotFiles(allDotFiles, format);
                System.out.println("Successfully generated " + format + " diagrams for " + allDotFiles.size() + " files");
            } catch (IOException e) {
                System.err.println("Error generating " + format + " diagrams: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isGraphvizInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-V");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recursively prepares all DOT files by extracting steps.
     *
     * @param sourceDir   The source directory containing DOT files
     * @param outputDir   The output directory to write generated files to
     * @param format      The format to generate the diagrams in (PNG or SVG)
     * @param allDotFiles List to collect all DOT files that need to be processed
     * @throws IOException If there is an error processing the files
     */
    private void prepareAllDotFiles(File sourceDir, File outputDir, ImageFormat format, List<File> allDotFiles) throws IOException {
        // Create the output directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Process all DOT files in the source directory
        File[] dotFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".dot"));

        if (dotFiles != null) {
            for (File dotFile : dotFiles) {
                // Create a subdirectory for the DOT file's steps
                String baseName = dotFile.getName().replace(".dot", "");
                File outputSubDir = new File(outputDir, baseName);
                outputSubDir.mkdirs();

                // Parse the DOT file to extract steps
                DotFile dotFileObj = new DotFile(dotFile);
                ParsedDotFile parsedFile = new DotStepParser().parse(dotFileObj);
                List<Step> steps = parsedFile.getSteps();

                // Generate DOT files for each step
                for (int i = 0; i < steps.size(); i++) {
                    Step step = steps.get(i);
                    String stepContent = step.getContent();

                    // Write the modified step content to a file
                    File stepFile = new File(outputSubDir, "step" + (i + 1) + ".dot");
                    Files.writeString(stepFile.toPath(), stepContent);

                    // Add to the list of files to process
                    allDotFiles.add(stepFile);
                }
            }
        }

        // Process subdirectories
        File[] subdirs = sourceDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                File outputSubDir = new File(outputDir, subdir.getName());
                outputSubDir.mkdirs();
                // Recursively process the subdirectory
                prepareAllDotFiles(subdir, outputSubDir, format, allDotFiles);
            }
        }
    }

    /**
     * Processes DOT files using Graphviz dot command.
     *
     * @param dotFiles List of DOT files to process
     * @param format   The format to generate the diagrams in (PNG or SVG)
     * @throws IOException If there is an error running Graphviz
     */
    private void processDotFiles(List<File> dotFiles, ImageFormat format) throws IOException {
        System.out.println("Processing " + dotFiles.size() + " DOT files with Graphviz");

        for (File dotFile : dotFiles) {
            String formatFlag = format == ImageFormat.SVG ? "-Tsvg" : "-Tpng";
            String outputExtension = format == ImageFormat.SVG ? ".svg" : ".png";
            
            // Generate output filename
            String baseName = dotFile.getName().replace(".dot", "");
            File outputFile = new File(dotFile.getParent(), baseName + outputExtension);

            // Build Graphviz command
            List<String> command = new ArrayList<>();
            command.add("dot");
            command.add(formatFlag);
            command.add("-o");
            command.add(outputFile.getAbsolutePath());
            command.add(dotFile.getAbsolutePath());

            try {
                // Execute Graphviz
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(dotFile.getParentFile());
                Process process = pb.start();
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    // Read error stream
                    String error = new String(process.getErrorStream().readAllBytes());
                    System.err.println("Error processing " + dotFile.getName() + ": " + error);
                } else {
                    System.out.println("Generated " + outputFile.getName());
                }
            } catch (Exception e) {
                System.err.println("Failed to process " + dotFile.getName() + ": " + e.getMessage());
            }
        }
    }
} 