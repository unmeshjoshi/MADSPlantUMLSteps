package com.pumlsteps.presentation;

import java.util.List;

public class PresentationConfig {
    private String title;
    private List<SlideConfig> slides;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<SlideConfig> getSlides() {
        return slides;
    }

    public void setSlides(List<SlideConfig> slides) {
        this.slides = slides;
    }
}
