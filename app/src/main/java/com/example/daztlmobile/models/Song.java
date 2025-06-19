package com.example.daztlmobile.models;

public class Song {
    public int id;
    public String title;
    public String artistName;
    public String audioUrl;
    public String coverUrl;
    public String releaseDate;

    public static final String BASE_URL = "http://10.0.2.2:8000";

    public String getFullAudioUrl() {
        return audioUrl.replace("localhost", "10.0.2.2");
    }

    public String getFullCoverUrl() {
        return coverUrl.replace("localhost", "10.0.2.2");
    }

    public void setFullCoverUrl(String url){
        this.coverUrl = url.replace("localhost", "10.0.2.2");
    }
}
