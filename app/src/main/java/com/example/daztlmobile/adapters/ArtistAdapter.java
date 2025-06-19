package com.example.daztlmobile.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.daztlmobile.R;
import com.example.daztlmobile.models.Artist;

import java.util.ArrayList;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
        void onLikeClick(Artist artist, int position);
    }

    private List<Artist> artists = new ArrayList<>();
    private final OnArtistClickListener listener;

    public ArtistAdapter(OnArtistClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist_card, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);
        holder.tvArtistName.setText(artist.name);

        if (artist.profilePicture != null && !artist.profilePicture.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(artist.profilePicture)
                    .placeholder(R.drawable.ic_music_note)
                    .into(holder.ivArtistImage);
        }

        holder.ivLike.setImageResource(artist.isLiked ?
                R.drawable.ic_lke_blue : R.drawable.ic_like);

        // Configurar listeners
        holder.itemView.setOnClickListener(v -> listener.onArtistClick(artist));
        holder.ivLike.setOnClickListener(v -> listener.onLikeClick(artist, position));
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
        notifyDataSetChanged();
    }

    public void updateArtistLikeStatus(int position, boolean isLiked) {
        if (position >= 0 && position < artists.size()) {
            artists.get(position).isLiked = isLiked;
            notifyItemChanged(position);
        }
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtistImage;
        TextView tvArtistName;
        ImageView ivLike;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtistImage = itemView.findViewById(R.id.ivArtistImage);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            ivLike = itemView.findViewById(R.id.ivLike);
        }
    }
}