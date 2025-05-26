package com.example.daztlmobile.models;

import java.util.List;

public class Album {
    public int id;
    public String title;
    public int artist;
    public String artist_name;
    public String cover_image;
    public List<Song> songs;

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist=" + artist +
                ", artist_name='" + artist_name + '\'' +
                ", cover_image='" + cover_image + '\'' +
                ", songs=" + songs +
                '}';
    }
}
