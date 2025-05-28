package com.pumlsteps.presentation;

import java.util.List;

public class PresentationConfig {
    private String title;
    private String description;
    private String icon;
    private String category;
    private List<SlideConfig> slides;
    private List<SectionConfig> sections;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<SlideConfig> getSlides() {
        return slides;
    }

    public void setSlides(List<SlideConfig> slides) {
        this.slides = slides;
    }
    
    public List<SectionConfig> getSections() {
        return sections;
    }
    
    public void setSections(List<SectionConfig> sections) {
        this.sections = sections;
    }
}
