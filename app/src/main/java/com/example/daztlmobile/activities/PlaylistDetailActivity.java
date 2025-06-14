package com.example.daztlmobile.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.SongAdapter;
import com.example.daztlmobile.adapters.SongAdapterWithAddButton;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.services.MusicPlaybackService;
import com.example.daztlmobile.utils.SessionManager;
import com.example.daztlmobile.utils.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.List;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;

public class PlaylistDetailActivity extends AppCompatActivity {

    private TextView tvPlaylistName;
    private EditText etSearch;
    private RecyclerView rvSongs;
    private SongAdapterWithAddButton adapter;

    private RecyclerView rvPlaylistSongs;
    private SongAdapter adapterPlaylistSongs;

    private List<Song> allSongs = new ArrayList<>();
    private List<Song> playlistSongs = new ArrayList<>();

    private String playlistName;
    private int playlistId;

    private MusicPlaybackService musicService;
    private boolean bound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.LocalBinder binder = (MusicPlaybackService.LocalBinder) service;
            musicService = binder.getService();
            bound = true;

            musicService.setPlaylist(playlistSongs);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            musicService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        // Recibir datos de la playlist
        playlistName = getIntent().getStringExtra("playlist_name");
        playlistId = getIntent().getIntExtra("playlist_id", -1);

        tvPlaylistName = findViewById(R.id.tvPlaylistName);
        etSearch = findViewById(R.id.etSearchSongs);
        rvSongs = findViewById(R.id.rvSearchResults);
        rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);

        tvPlaylistName.setText(playlistName);

        adapter = new SongAdapterWithAddButton(this::onAddSongClicked);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        rvSongs.setAdapter(adapter);

        adapterPlaylistSongs = new SongAdapter(song -> {
            if (bound && musicService != null) {
                int index = playlistSongs.indexOf(song);
                if (index != -1) {
                    musicService.setPlaylist(playlistSongs);
                    musicService.playSong(index);
                }
            }
        });
        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
        rvPlaylistSongs.setAdapter(adapterPlaylistSongs);

        loadAllSongs();
        loadPlaylistSongs();

        etSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                filterSongs(s.toString());
            }
        });

        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void loadAllSongs() {
        new Thread(() -> {
            try {
                var stub = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel());
                var response = stub.listSongs(DaztlServiceOuterClass.Empty.newBuilder().build());

                allSongs.clear();
                for (var sr : response.getSongsList()) {
                    Song s = new Song();
                    s.id = sr.getId();
                    s.title = sr.getTitle();
                    s.artistName = sr.getArtist();
                    s.audioUrl = sr.getAudioUrl();
                    s.coverUrl = sr.getCoverUrl();
                    s.releaseDate = sr.getReleaseDate();
                    allSongs.add(s);
                }

                runOnUiThread(() -> adapter.setSongs(allSongs));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error cargando canciones", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadPlaylistSongs() {
        new Thread(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                String token = sessionManager.fetchToken();

                var stub = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel());
                var request = DaztlServiceOuterClass.PlaylistDetailRequest.newBuilder()
                        .setToken(token)
                        .setPlaylistId(playlistId)
                        .build();

                var response = stub.getPlaylistDetail(request);

                if (response.getStatus().equalsIgnoreCase("success")) {
                    playlistSongs.clear();
                    for (var sr : response.getSongsList()) {
                        Song s = new Song();
                        s.id = sr.getId();
                        s.title = sr.getTitle();
                        s.artistName = sr.getArtist();
                        s.audioUrl = sr.getAudioUrl();
                        s.coverUrl = sr.getCoverUrl();
                        s.releaseDate = sr.getReleaseDate();
                        playlistSongs.add(s);
                    }

                    runOnUiThread(() -> {
                        adapterPlaylistSongs.setSongs(playlistSongs);
                        if (bound && musicService != null) {
                            musicService.setPlaylist(playlistSongs);
                        }
                    });

                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + response.getMessage(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error cargando playlist", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void filterSongs(String query) {
        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.title.toLowerCase().contains(query.toLowerCase()) ||
                    song.artistName.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(song);
            }
        }
        adapter.setSongs(filtered);
    }

    private void onAddSongClicked(Song song) {
        if (playlistId == -1) {
            Toast.makeText(this, "ID de playlist inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.fetchToken();

        DaztlServiceOuterClass.AddSongToPlaylistRequest request = DaztlServiceOuterClass
                .AddSongToPlaylistRequest.newBuilder()
                .setToken(token)
                .setPlaylistId(playlistId)
                .setSongId(song.id)
                .build();

        new Thread(() -> {
            try {
                var stub = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel());
                var response = stub.addSongToPlaylist(request);

                runOnUiThread(() -> {
                    if (response.getStatus().equalsIgnoreCase("success")) {
                        Toast.makeText(this, "Agregado: " + song.title, Toast.LENGTH_SHORT).show();
                        loadPlaylistSongs();
                    } else {
                        Toast.makeText(this, "Error: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}
