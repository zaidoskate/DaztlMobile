package com.example.daztlmobile.models;

import java.util.List;

public class RecommendedSongsResponse {
    private List<Song> recommended;

    public List<Song> getRecommended() {
        return recommended;
    }

    public void setRecommended(List<Song> recommended) {
        this.recommended = recommended;
    }
}
