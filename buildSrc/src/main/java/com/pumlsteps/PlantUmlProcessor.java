package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlantUmlProcessor {
    private final StepParser parser;
    private final StepImageGenerator stepImageGenerator;
    private final PptGenerator pptGenerator;

    public PlantUmlProcessor(String plantUmlJarPath) {
        this.parser = new StepParser();
        this.stepImageGenerator = new StepImageGenerator(plantUmlJarPath);
        this.pptGenerator = new PptGenerator();
    }


    public static class PumlFile {
        private static final List<String> IGNORED_FILES = List.of("style.puml");
        private final File file;

        public PumlFile(File file) {
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
            }
            if (!file.getName().endsWith(".puml")) {
                throw new IllegalArgumentException("Not a PUML file: " + file.getName());
            }
            this.file = file;
        }

        public static List<PumlFile> find(File sourceDir) {
            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                throw new IllegalArgumentException("Invalid source directory: " + sourceDir.getAbsolutePath());
            }

            File[] files = sourceDir.listFiles((dir, name) ->
                    name.endsWith(".puml") && !IGNORED_FILES.contains(name)
            );

            if (files == null) {
                return Collections.emptyList();
            }

            return Arrays.stream(files)
                    .map(PumlFile::new)
                    .collect(Collectors.toList());
        }

        public File createSubDirectory(File parentDir) throws IOException {
            File subDir = new File(parentDir, getBaseName());
            if (!subDir.exists() && !subDir.mkdirs()) {
                throw new IOException("Failed to create subdirectory: " + subDir.getAbsolutePath());
            }
            return subDir;
        }

        public String getBaseName() {
            return file.getName().replace(".puml", "");
        }

        public File getFile() {
            return file;
        }

        public List<String> readLines() throws IOException {
            return Files.readAllLines(file.toPath());
        }

        @Override
        public String toString() {
            return file.getAbsolutePath();
        }
    }


    private List ignoredFiles = Arrays.asList("style.puml");
    public void process(File sourceDir, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        var pumlFiles = PumlFile.find(sourceDir);

        if (pumlFiles.isEmpty()) {
            return;
        }

        for (PumlFile sourcePumlFile : pumlFiles) {

            File subDir = sourcePumlFile.createSubDirectory(outputDir);

            System.out.println("Processing file = " + sourcePumlFile.getFile().getAbsolutePath());
            var parsedPumlFile = parser.parse(sourcePumlFile);
            var generatedSteps
                    = stepImageGenerator.generateDiagrams(parsedPumlFile.getSteps(), subDir);

            System.out.println("steps = " + generatedSteps.size());

            String sectionName = sourcePumlFile.getBaseName();
            pptGenerator.addSlide(sectionName, generatedSteps);
            generateStepHtml(subDir,  generatedSteps);

        }

        pptGenerator.save(new File(outputDir, "all_steps.pptx").getAbsolutePath());
        generateRootHtml(outputDir);
    }


    private void generateStepHtml(File subDir, List<GeneratedStep> steps) throws IOException {
        int totalSteps = steps.size(); // Get the actual number of steps

        String htmlContent = String.format("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Step Viewer</title>
            <style>
                body { font-family: Arial, sans-serif; text-align: center; margin: 20px; }
                img { width: 80%%; max-width: 800px; height: auto; border: 1px solid #ccc; }
                button { padding: 10px 20px; margin: 10px; font-size: 16px; cursor: pointer; }
                button:disabled { background-color: #ccc; cursor: not-allowed; }
            </style>
        </head>
        <body>
            <h1>Step Viewer</h1>
            <img id="diagram" src="step1.png" alt="Step Diagram">
            <br>
            <button id="prevButton" onclick="prevStep()" disabled>Previous</button>
            <button id="nextButton" onclick="nextStep()">Next</button>
            <script>
                const totalSteps = %d ;
                let currentStep = 1;
                const diagramElement = document.getElementById('diagram');
                const prevButton = document.getElementById('prevButton');
                const nextButton = document.getElementById('nextButton');

                function updateDiagram() {
                    diagramElement.src = `step${currentStep}.png`;
                    prevButton.disabled = currentStep === 1;
                    nextButton.disabled = currentStep === totalSteps;
                }

                function prevStep() {
                    if (currentStep > 1) {
                        currentStep--;
                        updateDiagram();
                    }
                }

                function nextStep() {
                    if (currentStep < totalSteps) {
                        currentStep++;
                        updateDiagram();
                    }
                }
            </script>
        </body>
        </html>
    """, totalSteps); // Replace %d with the actual value of steps.size()

        // Write the HTML content to the file
        File htmlFile = new File(subDir, "index.html");
        Files.writeString(htmlFile.toPath(), htmlContent);
    }

    private void generateRootHtml(File rootDir) throws IOException {
        // Find all subdirectories in the rootDir
        File[] subDirs = rootDir.listFiles(File::isDirectory);
        if (subDirs == null || subDirs.length == 0) {
            throw new IOException("No subdirectories found in: " + rootDir.getAbsolutePath());
        }

        StringBuilder htmlContent = new StringBuilder();

        // Start HTML
        htmlContent.append("""
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Root Step Viewer</title>
            <style>
                body { font-family: Arial, sans-serif; text-align: center; margin: 20px; }
                a { display: block; margin: 10px 0; font-size: 18px; text-decoration: none; color: #007BFF; }
                a:hover { text-decoration: underline; }
            </style>
        </head>
        <body>
            <h1>Root Step Viewer</h1>
            <ul>
    """);

        // Add links for each subdirectory
        for (File subDir : subDirs) {
            String subDirName = subDir.getName();
            htmlContent.append(String.format("""
            <li><a href="%s/index.html">%s</a></li>
        """, subDirName, subDirName));
        }

        // Close HTML
        htmlContent.append("""
            </ul>
        </body>
        </html>
    """);

        // Write the HTML content to the root directory
        File rootHtmlFile = new File(rootDir, "index.html");
        Files.writeString(rootHtmlFile.toPath(), htmlContent.toString());
    }


}
