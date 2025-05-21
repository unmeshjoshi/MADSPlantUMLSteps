package com.pumlsteps.presentation;

import java.util.List;

public class SlideConfig {
    private String title;
    private String type;  // "text", "diagram", etc.
    private List<String> bullets;
    private String diagramRef;
    private String notes;

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
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
