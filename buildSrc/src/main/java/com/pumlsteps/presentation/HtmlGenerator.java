package com.pumlsteps.presentation;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HtmlGenerator {
    private void generateBulletList(StringBuilder htmlContent, List<String> bullets) {
        if (bullets != null && !bullets.isEmpty()) {
            htmlContent.append("<ul>");
            for (String bullet : bullets) {
                htmlContent.append("<li>").append(bullet).append("</li>");
            }
            htmlContent.append("</ul>");
        }
    }
    public void generateHtmlFromYaml(File yamlFile, File outputHtmlFile) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlData = yaml.loadAs(new FileReader(yamlFile), Map.class);

        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>")
                .append(yamlData.get("title"))
                .append("</title>")
                .append("<link rel=\"stylesheet\" href=\"styles.css\">")
                .append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">")
                .append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>")
                .append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Poppins:wght@500;600;700&display=swap\" rel=\"stylesheet\">")
                .append("<link href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css\" rel=\"stylesheet\">")
                .append("</head>")
                .append("<body>");
        htmlContent.append("<div class='slide-container'>");

        List<Map<String, Object>> slides = (List<Map<String, Object>>) yamlData.get("slides");

        for (Map<String, Object> slide : slides) {
            htmlContent.append("<div class='slide'>")
                    .append("<div class='title-container'>")
                    .append("<h2>")
                    .append(slide.get("title"))
                    .append("</h2>")
                    .append(slide.containsKey("notes") ? "<div class='title-notes-indicator'><i class='fas fa-sticky-note'></i></div>" : "")
                    .append("</div>")
                    .append("<div class='content-container'>");

            if ("text".equals(slide.get("type"))) {
                generateBulletList(htmlContent, (List<String>) slide.get("bullets"));
            } else if ("diagram".equals(slide.get("type"))) {
                // Add full-size class if no bullets
                String fullSizeClass = !slide.containsKey("bullets") ? " full-size" : "";
                htmlContent.append("<div class='diagram-slide-content" + fullSizeClass + "'>");
                
                // Diagram container
                htmlContent.append("<div class='diagram-container'>");
                String diagramRef = (String) slide.get("diagramRef");
                File diagramDir = new File("build/diagrams/" + diagramRef);
                File[] stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".svg"));

                if (stepFiles != null) {
                    for (File stepFile : stepFiles) {
                        String stepName = stepFile.getName();
                        String stepIndex = stepName.replaceAll("[^0-9]", ""); // Extract the step number from the filename
                        htmlContent.append("<div class='step' style='display: none;' step_index='")
                                .append(stepIndex)
                                .append("'><img src='build/diagrams/")
                                .append(diagramRef)
                                .append("/")
                                .append(stepName)
                                .append("' alt='")
                                .append(diagramRef)
                                .append(" ")
                                .append(stepName)
                                .append("'></div>");
                    }
                }
                htmlContent.append("</div>"); // Close diagram-container

                // Add bullet points if they exist
                if (slide.containsKey("bullets")) {
                    htmlContent.append("<div class='diagram-bullets'>");
                    List<String> bullets = (List<String>) slide.get("bullets");
                    htmlContent.append("<ul>");
                    for (String bullet : bullets) {
                        htmlContent.append("<li>").append(bullet).append("</li>");
                    }
                    htmlContent.append("</ul>");
                    htmlContent.append("</div>");
                }

                htmlContent.append("</div>"); // Close diagram-slide-content
            }
            // Add speaker notes if present
            if (slide.containsKey("notes")) {
                htmlContent.append("<div class='speaker-notes'>")
                        .append("<div class='notes-icon'><i class='fas fa-sticky-note'></i></div>")
                        .append("<div class='notes-content'>")
                        .append(slide.get("notes"))
                        .append("</div>")
                        .append("</div>");
            }
            htmlContent.append("</div></div>"); // Close content-container and slide div
        }

        htmlContent.append("</div>"); //close slide-container

        // Add navigation buttons
        htmlContent.append("<div class='controls'>")
                .append("<button id='prev-button' onclick='prevStep()' disabled>")
                .append("<i class='fas fa-chevron-left'></i>")
                .append("<span>Previous</span>")
                .append("</button>")
                .append("<button id='next-button' onclick='nextStep()'>")
                .append("<span>Next</span>")
                .append("<i class='fas fa-chevron-right'></i>")
                .append("</button>")
                .append("</div>");

        // Link to the external JavaScript file
        htmlContent.append("<script src='presentation.js'></script>");

        htmlContent.append("</body></html>");

        try (FileWriter writer = new FileWriter(outputHtmlFile)) {
            writer.write(htmlContent.toString());
        }
    }
}
