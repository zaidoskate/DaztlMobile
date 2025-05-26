package com.example.daztlmobile.models;

public class ArtistProfile {
    public int id;
    public UserResponse user;
    public String bio;
    public String profile_picture;

    @Override
    public String toString() {
        return "ArtistProfile{" +
                "id=" + id +
                ", user=" + user +
                ", bio='" + bio + '\'' +
                ", profile_picture='" + profile_picture + '\'' +
                '}';
    }
}
