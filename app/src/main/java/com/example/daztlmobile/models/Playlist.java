package com.example.daztlmobile.models;

import java.util.List;

public class Playlist {
    public int id;
    public String name;
    public List<Integer> songs;
    public String created_at;

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", songs=" + songs +
                ", created_at='" + created_at + '\'' +
                '}';
    }
}
