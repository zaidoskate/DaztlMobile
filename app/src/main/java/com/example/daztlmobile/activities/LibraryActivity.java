package com.example.daztlmobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.PlaylistAdapter;
import com.example.daztlmobile.models.Playlist;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import daztl.MusicServiceGrpc;

public class LibraryActivity extends AppCompatActivity {

    private RecyclerView rvPlaylists;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlistList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        MaterialToolbar toolbar = findViewById(R.id.libraryToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvPlaylists = findViewById(R.id.rvPlaylists);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        /*playlistAdapter = new PlaylistAdapter(playlistList, playlist -> {
            Intent intent = new Intent(LibraryActivity.this, PlaylistDetailActivity.class);
            intent.putExtra("playlist_id", playlist.getId());
            intent.putExtra("playlist_name", playlist.getName());
            startActivity(intent);
        });
        rvPlaylists.setAdapter(playlistAdapter);

        loadUserPlaylists();*/
    }

    private void loadUserPlaylists() {
        /*Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                var channel = GrpcClient.getChannel();
                var stub = MusicServiceGrpc.newBlockingStub(channel);
                var token = sessionManager.fetchToken();

                var request = daztl.DaztlServiceOuterClass.PlaylistListRequest.newBuilder()
                        .setToken(token)
                        .build();

                var response = stub.listPlaylists(request);

                List<Playlist> playlists = new ArrayList<>();
                for (var pl : response.getPlaylistsList()) {
                    Playlist playlist = new Playlist(pl.getId(), pl.getName(), pl.getCoverUrl(), pl.get);
                    playlists.add(playlist);
                }

                runOnUiThread(() -> {
                    playlistList.clear();
                    playlistList.addAll(playlists);
                    playlistAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error al cargar playlists", Toast.LENGTH_SHORT).show());
            }
        });*/
    }
}
