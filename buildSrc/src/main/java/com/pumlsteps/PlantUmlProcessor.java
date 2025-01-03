package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class PlantUmlProcessor {
    private final StepParser parser;
    private final StepImageGenerator stepImageGenerator;
    private final PptGenerator pptGenerator;

    public PlantUmlProcessor(String plantUmlJarPath) {
        this.parser = new StepParser();
        this.stepImageGenerator = new StepImageGenerator(plantUmlJarPath);
        this.pptGenerator = new PptGenerator();
    }

    private List ignoredFiles = Arrays.asList("style.puml");
    public void process(File sourceDir, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        File[] pumlFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".puml") && !ignoredFiles.contains(name));
        if (pumlFiles == null) return;

        for (File sourceFile : pumlFiles) {
            String sectionName = sourceFile.getName().replace(".puml", "");
            File subDir = new File(outputDir, sectionName);
            if (!subDir.exists() && !subDir.mkdirs()) {
                throw new IOException("Failed to create subdirectory: " + subDir.getAbsolutePath());
            }

            System.out.println("Processing file = " + sourceFile.getAbsolutePath());
            var pumlFile = parser.parse(sourceFile);

            for (Step step : pumlFile.getSteps()) {
                File stepFile = new File(subDir, "step" + step.getStepNumber() + ".puml");
                Files.writeString(stepFile.toPath(), step.getContent());
                step.setFilePath(stepFile.getAbsolutePath());
            }
            stepImageGenerator.generateDiagram(subDir, subDir);
            System.out.println("steps = " + pumlFile.getSteps().size());
            pptGenerator.addSlide(sectionName, pumlFile.getSteps());
            generateStepHtml(subDir,  pumlFile.getSteps());

        }

        pptGenerator.save(new File(outputDir, "all_steps.pptx").getAbsolutePath());
        generateRootHtml(outputDir);
    }


    private void generateStepHtml(File subDir, List<Step> steps) throws IOException {
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
