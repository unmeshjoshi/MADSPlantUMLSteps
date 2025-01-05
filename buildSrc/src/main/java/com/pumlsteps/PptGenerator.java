package com.pumlsteps;

import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;

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
    private void addStepSlide(String sectionName, String title, File imageFile) {
        try {
            // Get slide dimensions
            int slideWidth = ppt.getPageSize().width;
            int slideHeight = ppt.getPageSize().height;

            // Load the image to get its dimensions
            assert imageFile.exists();

            BufferedImage image = ImageIO.read(imageFile);
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            // Calculate scale factors
            double scaleX = (double) slideWidth / imageWidth;
            double scaleY = (double) slideHeight / imageHeight;
            double scaleFactor = Math.min(scaleX, scaleY);

            // Calculate scaled dimensions
            int scaledWidth = (int) (imageWidth * scaleFactor);
            int scaledHeight = (int) (imageHeight * scaleFactor);

            // Center the image
            int xPosition = (slideWidth - scaledWidth) / 2;
            int yPosition = (slideHeight - scaledHeight) / 2;

            // Create a new slide
            XSLFSlide slide = ppt.createSlide();

            // Add the title
            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setText(title);
            titleBox.setAnchor(new Rectangle(50, 20, slideWidth - 100, 50));
            titleBox.setHorizontalCentered(true);
            titleBox.getTextParagraphs().get(0).setBulletFontSize(24.0);

            // Add the image
            XSLFPictureData pictureData = ppt.addPicture(Files.readAllBytes(imageFile.toPath()), XSLFPictureData.PictureType.PNG);
            slide.createPicture(pictureData).setAnchor(new Rectangle(xPosition, yPosition, scaledWidth, scaledHeight));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding step slide: " + e.getMessage(), e);
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
