package com.example.daztlmobile.models;

public class Artist {
    public int id;
    public String name;
    public String profilePicture;
    public String bio;
    public boolean isLiked;

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public Artist() {}

    public Artist(int id, String name, String profilePicture) {
        this.id = id;
        this.name = name;
        this.profilePicture = profilePicture;
    }

    public static final String BASE_URL = "http://10.0.2.2:8000";
    public String getFullProfilePictureUrl() {
        return profilePicture.replace("localhost", "10.0.2.2");
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", bio='" + (bio != null ? bio : "") + '\'' +
                '}';
    }
}