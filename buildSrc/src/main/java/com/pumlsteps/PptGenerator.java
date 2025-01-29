package com.pumlsteps;

import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

public class PptGenerator {
    private final XMLSlideShow ppt;

    public PptGenerator() {
        this.ppt = new XMLSlideShow();
    }

    /**
     * Adds a section and its associated slides to the PowerPoint.
     *
     * @param sectionName The name of the section.
     * @param steps       The list of steps in the section.
     */
    public void addSlides(String sectionName, List<GeneratedStep> steps) {
        // Add a separator slide for the section
        addSeparatorSlide(sectionName);

        // Add slides for each step in the section
        for (GeneratedStep step : steps) {
            addStepSlide(sectionName, step.getTitle(), step.getPngFile());
        }
    }

    /**
     * Adds a separator slide for a section.
     *
     * @param sectionName The name of the section.
     */
    private void addSeparatorSlide(String sectionName) {
        try {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setText(sectionName);
            textBox.setAnchor(new Rectangle(50, 150, 860, 240)); // Center text in the slide
            textBox.setHorizontalCentered(true);
            textBox.setVerticalAlignment(VerticalAlignment.MIDDLE);
            textBox.getTextParagraphs().get(0).setBulletFontSize(36.0);
        } catch (Exception e) {
            throw new RuntimeException("Error adding separator slide: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a slide for an individual step.
     *
     * @param sectionName The section name.
     * @param title       The title for the step.
     * @param imageFile   The image file for the step diagram.
     */
    public void addStepSlide(String sectionName, String title, File imageFile) {
        try {
            // Get slide dimensions
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;

            // Load the image to get its dimensions
            assert imageFile.exists();

            BufferedImage image = ImageIO.read(imageFile);


            // Create a new slide
            XSLFSlide slide = ppt.createSlide();

            // Add the title
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(50, 20, slideWidth - 100, 50));
            titleBox.setHorizontalCentered(true);

            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Bitter");
            titleRun.setFontSize(26.0);


            // Calculate remaining area for image
            int imageY = 50;  // Below title
            int imageHeight = slideHeight - imageY - 20;  // Leave margin at bottom

            double scaleX = (double) (slideWidth - 200) / image.getWidth();
            double scaleY = (double) imageHeight / image.getHeight();
            double scale = Math.min(scaleX, scaleY);

            int scaledWidth = (int) (image.getWidth() * scale);
            int scaledHeight = (int) (image.getHeight() * scale);

            // Center the image
            int xPosition = (slideWidth - scaledWidth) / 2;
            int yPosition = (slideHeight - scaledHeight) / 2;

            // Add the image
            XSLFPictureData pictureData = ppt.addPicture(Files.readAllBytes(imageFile.toPath()), XSLFPictureData.PictureType.PNG);
            slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding step slide: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a text slide with bullet points.
     *
     * @param title The title of the slide
     * @param bullets List of bullet points to add
     */
    public void addTextSlide(String title, List<String> bullets) {
        try {
            XSLFSlide slide = ppt.createSlide();
            
            // Add title
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle(50, 20, ppt.getPageSize().width - 100, 50));
            titleBox.setHorizontalCentered(true);
            
            // Create a text run to set font properties
            XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
            titleRun.setText(title);
            titleRun.setFontFamily("Bitter"); // Set the font for the title
            titleRun.setFontSize(28.0); // Set the


            // Add bullet points
            if (!bullets.isEmpty()) {
                XSLFTextBox bulletBox = slide.createTextBox();
                bulletBox.setAnchor(new Rectangle(50, 80, ppt.getPageSize().width - 100, 
                                                ppt.getPageSize().height - 100));

                for (String bullet : bullets) {
                    XSLFTextParagraph para = bulletBox.addNewTextParagraph();
                    para.setBullet(true); // Enable bullet points
                    para.setBulletFontSize(18.0);
                    XSLFTextRun run = para.addNewTextRun();
                    run.setFontFamily("Inter");
                    run.setText(bullet);
                    run.setFontSize(18.0);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error adding text slide: " + e.getMessage(), e);
        }
    }

    /**
     * Saves the PowerPoint to the specified file.
     *
     * @param filePath The file path to save the PowerPoint.
     */
    public void save(String filePath) {
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            ppt.write(out);
        } catch (Exception e) {
            throw new RuntimeException("Error saving presentation: " + e.getMessage(), e);
        }
    }
}
