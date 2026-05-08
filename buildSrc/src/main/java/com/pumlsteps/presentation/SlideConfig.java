package com.pumlsteps.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SlideConfig {
    @JsonProperty("title")
    public String title;
    
    @JsonProperty("type")
    public String type;  // "text", "diagram", etc.
    
    @JsonProperty("bullets")
    public List<String> bullets;
    
    @JsonProperty("diagramRef")
    public String diagramRef;
    
    @JsonProperty("imagePath")
    public String imagePath;
    
    @JsonProperty("notes")
    public String notes;
    
    @JsonProperty("showBullets")
    public boolean showBullets = true;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getBullets() {
        return bullets;
    }

    public void setBullets(List<String> bullets) {
        this.bullets = bullets;
    }

    public String getDiagramRef() {
        return diagramRef;
    }

    public void setDiagramRef(String diagramRef) {
        this.diagramRef = diagramRef;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isShowBullets() {
        return showBullets;
    }

    public void setShowBullets(boolean showBullets) {
        this.showBullets = showBullets;
    }
}
