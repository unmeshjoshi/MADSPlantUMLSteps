package com.pumlsteps.presentation;

import com.pumlsteps.GeneratedStep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class RevealJsGenerator {
    private static final String REVEAL_JS_VERSION = "4.5.0";
    private static final String REVEAL_JS_CDN = "https://cdn.jsdelivr.net/npm/reveal.js@" + REVEAL_JS_VERSION;

    public void generatePresentation(File outputDir, String title, List<List<GeneratedStep>> sections) throws IOException {
        StringBuilder slides = new StringBuilder();
        
        // Generate slides for each section
        for (List<GeneratedStep> section : sections) {
            slides.append(generateSection(section));
        }

        String htmlContent = String.format("""
            <!doctype html>
            <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <link rel="stylesheet" href="%s/dist/reveal.css">
                    <link rel="stylesheet" href="%s/dist/theme/white.css">
                    <style>
                        .reveal .slides section img {
                            max-height: 80vh;
                            width: auto;
                        }
                        .slide-number {
                            font-size: 24px !important;
                        }
                        .controls {
                            bottom: 20px !important;
                        }
                    </style>
                </head>
                <body>
                    <div class="reveal">
                        <div class="slides">
                            %s
                        </div>
                    </div>
                    <script src="%s/dist/reveal.js"></script>
                    <script>
                        Reveal.initialize({
                            hash: true,
                            slideNumber: true,
                            navigationMode: 'linear',
                            width: '100%%',
                            height: '100%%',
                            margin: 0.1,
                            minScale: 0.2,
                            maxScale: 2.0,
                            keyboard: {
                                32: null // Disable space bar navigation
                            }
                        });
                    </script>
                </body>
            </html>
            """, 
            title,
            REVEAL_JS_CDN,
            REVEAL_JS_CDN,
            slides.toString(),
            REVEAL_JS_CDN
        );

        File presentationFile = new File(outputDir, "presentation.html");
        Files.writeString(presentationFile.toPath(), htmlContent);
    }

    private String generateSection(List<GeneratedStep> steps) throws IOException {
        StringBuilder section = new StringBuilder();
        
        for (GeneratedStep step : steps) {
            String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(step.getImageFile().toPath()));
            section.append(String.format("""
                <section>
                    <img src="data:image/png;base64,%s" alt="Step Diagram">
                    <aside class="notes">%s</aside>
                </section>
                """,
                base64Image,
                step.getTitle() != null ? step.getTitle() : ""
            ));
        }
        
        return section.toString();
    }
}
