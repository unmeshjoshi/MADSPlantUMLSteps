package com.pumlsteps.presentation;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HtmlGenerator {
    public void generateHtmlFromYaml(File yamlFile, File outputHtmlFile) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> yamlData = yaml.loadAs(new FileReader(yamlFile), Map.class);

        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>")
                .append(yamlData.get("title"))
                .append("</title>")
                .append("<link rel=\"stylesheet\" href=\"styles.css\">")
                .append("</head>")
                .append("<body>");
        htmlContent.append("<div class='slide-container'>");

        List<Map<String, Object>> slides = (List<Map<String, Object>>) yamlData.get("slides");

        for (Map<String, Object> slide : slides) {
            htmlContent.append("<div class='slide'>")
                    .append("<div class='title-container'><h2>")
                    .append(slide.get("title"))
                    .append("</h2></div>")
                    .append("<div class='content-container'>");

            if ("text".equals(slide.get("type"))) {
                List<String> bullets = (List<String>) slide.get("bullets");
                htmlContent.append("<ul>");
                for (String bullet : bullets) {
                    htmlContent.append("<li>").append(bullet).append("</li>");
                }
                htmlContent.append("</ul>");
            } else if ("diagram".equals(slide.get("type"))) {
                String diagramRef = (String) slide.get("diagramRef");
                File diagramDir = new File("build/diagrams/" + diagramRef);
                File[] stepFiles = diagramDir.listFiles((dir, name) -> name.endsWith(".png"));

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
            }
            htmlContent.append("</div></div>"); // Close content-container and slide div
        }

        htmlContent.append("</div>"); //close slide-container

        // Add navigation buttons
        htmlContent.append("<div class='nav-buttons'>")
                .append("<button id='prev-button' onclick='prevSlide()'>Previous</button>")
                .append("<button id='next-button' onclick='nextStep()'>Next</button>")
                .append("</div>");

        // Link to the external JavaScript file
        htmlContent.append("<script src='presentation.js'></script>");

        htmlContent.append("</body></html>");

        try (FileWriter writer = new FileWriter(outputHtmlFile)) {
            writer.write(htmlContent.toString());
        }
    }
}
