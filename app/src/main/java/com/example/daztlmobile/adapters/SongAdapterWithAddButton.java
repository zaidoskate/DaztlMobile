package com.example.daztlmobile.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.daztlmobile.R;
import com.example.daztlmobile.models.Song;

import java.util.ArrayList;
import java.util.List;

public class SongAdapterWithAddButton extends RecyclerView.Adapter<SongAdapterWithAddButton.SongViewHolder> {

    public interface OnAddClickListener {
        void onAddClick(Song song);
    }

    private List<Song> songs = new ArrayList<>();
    private final OnAddClickListener listener;

    public SongAdapterWithAddButton(OnAddClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_with_add, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(song.title);
        holder.tvArtist.setText(song.artistName);
        Glide.with(holder.itemView.getContext()).load(song.getFullCoverUrl()).into(holder.ivCover);
        holder.btnAdd.setOnClickListener(v -> listener.onAddClick(song));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvArtist;
        ImageButton btnAdd;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnAdd = itemView.findViewById(R.id.btnAddSong);
        }
    }
}
