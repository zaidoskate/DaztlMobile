package com.example.daztlmobile.models;

import java.util.List;

public class Playlist {
    public int id;
    public String name;
    public String coverUrl;
    public List<Song> songs;
    public String createdAt;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Playlist() {}

    public Playlist(int id, String name, String coverUrl, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.songs = songs;
    }

    public static final String BASE_URL = "http://10.0.2.2:8000";
    public String getFullCoverUrl() {
        return coverUrl.replace("localhost", "10.0.2.2");
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", songs=" + (songs != null ? songs.size() : 0) + " songs" +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}