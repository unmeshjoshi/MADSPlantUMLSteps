package com.pumlsteps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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


    public void process(File sourceDir, File outputDir) throws IOException {
        checkAndCreate(outputDir);

        PumlFile.find(sourceDir)
                .forEach(pumlFile ->
                        processSinglePumlFile(outputDir, pumlFile));

        generatePpt(outputDir);
        generateRootHtml(outputDir);
    }

    private void generatePpt(File outputDir) {
        pptGenerator.save(new File(outputDir, "all_steps.pptx").getAbsolutePath());
    }

    private static void checkAndCreate(File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }
    }

    private void processSinglePumlFile(File outputDir, PumlFile sourcePumlFile) {
        try {
            System.out.println("Processing file = " + sourcePumlFile.getFile().getAbsolutePath());

            var parsedPumlFile = parseSteps(sourcePumlFile);

            File subDir = sourcePumlFile.createSubDirectory(outputDir);
            var generatedSteps
                    = generateStepDiagrams(parsedPumlFile, subDir);

            System.out.println("steps = " + generatedSteps.size());

            String sectionName = sourcePumlFile.getBaseName();
//            generateStepSlides(sectionName, generatedSteps);
            generateStepHtml(subDir, generatedSteps);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<GeneratedStep> generateStepDiagrams(ParsedPlantUmlFile parsedPumlFile, File subDir) throws IOException {
        return stepImageGenerator.generateDiagrams(parsedPumlFile.getSteps(), subDir);
    }

    private ParsedPlantUmlFile parseSteps(PumlFile sourcePumlFile) throws IOException {
        var parsedPumlFile = parser.parse(sourcePumlFile);
        return parsedPumlFile;
    }

    private void generateStepSlides(String sectionName, List<GeneratedStep> generatedSteps) {
        pptGenerator.addSlides(sectionName, generatedSteps);
    }


    private void generateStepHtml(File subDir, List<GeneratedStep> steps) throws IOException {
        int totalSteps = steps.size(); // Get the actual number of steps

        String htmlContent = String.format("""
        <!DOCTYPE html>
        <html lang='en'>
        <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            <title>Step Viewer</title>
            <link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css'>
            <link rel='preconnect' href='https://fonts.googleapis.com'>
            <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>
            <link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap' rel='stylesheet'>
            <style>
                :root {
                    --primary: #2563eb;
                    --primary-light: #dbeafe;
                    --text: #1e293b;
                    --text-light: #64748b;
                    --border: #e2e8f0;
                    --surface: #ffffff;
                    --surface-hover: #f8fafc;
                }
                body { 
                    font-family: 'Inter', sans-serif; 
                    text-align: center; 
                    margin: 0; 
                    padding: 0;
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    color: var(--text);
                    background: var(--surface);
                }
                h1 { margin: 10px 0; }
                .image-container { 
                    margin: 10px auto;
                    padding: 10px;
                    background: #fff;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: calc(100vh - 120px);
                    width: 100%%;
                    max-width: 1200px;
                }
                object, img { 
                    width: 100%%;  /* Use full width of container */
                    max-width: 800px;  /* Maximum width on larger screens */
                    height: auto;  /* Maintain aspect ratio */
                    max-height: 80vh;  /* Prevent image from being too tall */
                    object-fit: contain;  /* Ensure image fits within bounds */
                    border: 1px solid #eee;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                /* Media query for smaller screens */
                @media screen and (max-width: 840px) {
                    .image-container {
                        padding: 5px;
                        height: calc(100vh - 100px);
                    }
                    object, img {
                        max-width: 95%%;  /* Use most of the screen width on mobile */
                        max-height: 70vh;
                    }
                    h1 {
                        font-size: 1.5em;
                        margin: 5px 0;
                    }
                }
                .controls {
                    padding: 24px;
                    display: flex;
                    gap: 16px;
                    justify-content: center;
                    align-items: center;
                    background: var(--surface);
                }
                button { 
                    display: inline-flex;
                    align-items: center;
                    gap: 8px;
                    padding: 12px 24px; 
                    font-family: 'Inter', sans-serif;
                    font-size: 14px;
                    font-weight: 500;
                    cursor: pointer;
                    background: var(--surface);
                    border: 1px solid var(--border);
                    border-radius: 8px;
                    color: var(--text);
                    transition: all 0.2s ease;
                    box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
                }
                button:hover:not(:disabled) { 
                    background: var(--surface-hover);
                    border-color: var(--primary);
                    color: var(--primary);
                    transform: translateY(-1px);
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                }
                button:disabled { 
                    background-color: var(--surface);
                    border-color: var(--border);
                    color: var(--text-light);
                    cursor: not-allowed;
                    transform: none;
                    box-shadow: none;
                }
                button i {
                    font-size: 16px;
                    transition: transform 0.2s ease;
                }
                button:hover:not(:disabled) i.fa-chevron-right {
                    transform: translateX(2px);
                }
                button:hover:not(:disabled) i.fa-chevron-left {
                    transform: translateX(-2px);
                }
            </style>
        </head>
        <body>
            <h1>Step Viewer</h1>
            <div class="image-container">
                <object id="diagram" type="image/svg+xml" data="step1.svg">
                    <img src="step1.svg" alt="Step Diagram">
                </object>
            </div>
            <div class='controls'>
                <button id='prevButton' onclick='prevStep()' disabled>
                    <i class='fas fa-chevron-left'></i>
                    <span>Previous</span>
                </button>
                <button id='nextButton' onclick='nextStep()'>
                    <span>Next</span>
                    <i class='fas fa-chevron-right'></i>
                </button>
            </div>
            <script>
                const totalSteps = %d ;
                let currentStep = 1;
                const diagramElement = document.getElementById('diagram');
                const prevButton = document.getElementById('prevButton');
                const nextButton = document.getElementById('nextButton');

                function updateDiagram() {
                    diagramElement.data = `step${currentStep}.svg`;
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
