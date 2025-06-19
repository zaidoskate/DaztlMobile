package com.example.daztlmobile.models;

import java.util.List;

public class Album {
    public int id;
    public String title;
    public String artistName;
    public String coverUrl;
    public List<Song> songs;

    public Album() {}

    public Album(int id, String title, String artistName, String coverUrl) {
        this.id = id;
        this.title = title;
        this.artistName = artistName;
        this.coverUrl = coverUrl;
    }

    public static final String BASE_URL = "http://10.0.2.2:8000";
    public String getFullCoverUrl() {
        return coverUrl.replace("localhost", "10.0.2.2");
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artistName='" + artistName + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", songs=" + (songs != null ? songs.size() : 0) + " songs" +
                '}';
    }
}