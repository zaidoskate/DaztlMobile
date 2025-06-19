package com.example.daztlmobile.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.SongAdapter;
import com.example.daztlmobile.adapters.SongAdapterWithAddButton;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.services.MusicPlaybackService;
import com.example.daztlmobile.utils.SessionManager;
import com.example.daztlmobile.utils.SimpleTextWatcher;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;

public class PlaylistDetailActivity extends AppCompatActivity
        implements SongAdapterWithAddButton.OnAddButtonClickListener, SongAdapter.OnSongClickListener {

    private ImageView ivPlaylistCover;
    private TextView tvPlaylistName;
    private TextView tvSongCount;
    private EditText etSearchSongs;
    private RecyclerView rvPlaylistSongs;
    private RecyclerView rvAvailableSongs;

    private SongAdapter playlistSongsAdapter;
    private SongAdapterWithAddButton availableSongsAdapter;

    private List<Song> playlistSongs = new ArrayList<>();
    private List<Song> allSongs = new ArrayList<>();

    private int playlistId;
    private String playlistName;
    private String playlistCoverUrl;

    private ExecutorService executor = Executors.newFixedThreadPool(2);

    private MusicPlaybackService musicService;
    private boolean bound = false;

    private ImageButton btnPlayPause, btnNext, btnPrev;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;
    private BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicPlaybackService.ACTION_PLAYBACK_STATUS.equals(intent.getAction())) {
                Log.d("Playback", "Recibido broadcast de estado de reproducción");
                runOnUiThread(() -> {
                    try {
                        if (bound && musicService != null) {
                            updateUIState();
                            if (musicService.isPlaying()) {
                                startSeekBarUpdate();
                            } else {
                                stopSeekBarUpdate();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Playback", "Error en onReceive", e);
                    }
                });
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.LocalBinder binder = (MusicPlaybackService.LocalBinder) service;
            musicService = binder.getService();
            bound = true;

            musicService.setOnPlaybackPreparedListener(() -> runOnUiThread(() -> {
                updateUIState();
                startSeekBarUpdate();
            }));

            if (musicService.isPlaying()) {
                updateUIState();
                startSeekBarUpdate();
            }

            if (!playlistSongs.isEmpty()) {
                musicService.setPlaylist(playlistSongs);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            musicService = null;
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getIntExtra("playlist_id", -1);
        playlistName = getIntent().getStringExtra("playlist_name");
        playlistCoverUrl = getIntent().getStringExtra("playlist_cover_url");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(playlistName);
        }

        initViews();
        setupPlaybackControls();
        setupAdapters();
        loadPlaylistDetails();
        loadAllSongs();
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MusicPlaybackService.ACTION_PLAYBACK_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }

        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(playbackReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("PlaylistDetail", "Receiver not registered", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound && musicService != null && !musicService.isPlaying()) {
            unbindService(connection);
            bound = false;
        }
    }

    private void initViews() {
        ivPlaylistCover = findViewById(R.id.ivPlaylistCover);
        tvPlaylistName = findViewById(R.id.tvPlaylistName);
        tvSongCount = findViewById(R.id.tvSongCount);
        rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);
        rvAvailableSongs = findViewById(R.id.rvAvailableSongs);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        seekBar = findViewById(R.id.seekBar);

        if (playlistCoverUrl != null && !playlistCoverUrl.isEmpty()) {
            Glide.with(this)
                    .load(playlistCoverUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .into(ivPlaylistCover);
        }

        tvPlaylistName.setText(playlistName);
    }

    private void setupAdapters() {
        playlistSongsAdapter = new SongAdapter(song -> {
            if (bound && musicService != null) {
                musicService.setPlaylist(playlistSongs);
                int index = playlistSongs.indexOf(song);
                if (index != -1) {
                    musicService.playSong(index);
                }
            }
        });
        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
        rvPlaylistSongs.setAdapter(playlistSongsAdapter);

        availableSongsAdapter = new SongAdapterWithAddButton(this);
        rvAvailableSongs.setLayoutManager(new LinearLayoutManager(this));
        rvAvailableSongs.setAdapter(availableSongsAdapter);

        etSearchSongs = findViewById(R.id.etSearchSongs);
        etSearchSongs.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                filterAvailableSongs(s.toString());
            }
        });
    }

    private void setupPlaybackControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (!bound || musicService == null) {
                Toast.makeText(this, "Servicio no disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                if (musicService.isPlaying()) {
                    musicService.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    stopSeekBarUpdate();
                } else {
                    if (musicService.getCurrentSong() == null && !playlistSongs.isEmpty()) {
                        musicService.setPlaylist(playlistSongs);
                        musicService.playSong(0);
                    }
                    musicService.resume();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    startSeekBarUpdate();
                }
            } catch (Exception e) {
                Log.e("Playback", "Error en controles", e);
                Toast.makeText(this, "Error al reproducir", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (!bound || musicService == null) return;
            musicService.playNext();
            updateUIState();
            startSeekBarUpdate();
        });

        btnPrev.setOnClickListener(v -> {
            if (!bound || musicService == null) return;
            musicService.playPrev();
            updateUIState();
            startSeekBarUpdate();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!bound || musicService == null) return;
                if (fromUser) {
                    musicService.seekTo(progress);
                    updateUIState();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onSongClick(Song song) {
        if (!bound || musicService == null) {
            return;
        }

        if (!playlistSongs.contains(song)) {
            Log.e("Playback", "Canción no encontrada en la playlist");
            return;
        }

        musicService.setOnPlaybackPreparedListener(() -> runOnUiThread(() -> {
            updateUIState();
            startSeekBarUpdate();
        }));

        musicService.setPlaylist(playlistSongs);
        int index = playlistSongs.indexOf(song);
        musicService.playSong(index);

        updateUIState();
        startSeekBarUpdate();
    }

    private void updateUIState() {
        if (!bound || musicService == null) {
            return;
        }

        Song current = musicService.getCurrentSong();
        if (current != null) {
            int pos = musicService.getCurrentPosition();
            int dur = musicService.getDuration();

            seekBar.setMax(dur);
            seekBar.setProgress(pos);

            btnPlayPause.setImageResource(musicService.isPlaying()
                    ? R.drawable.ic_pause
                    : R.drawable.ic_play);
        } else {
            seekBar.setProgress(0);
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate();
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (bound && musicService != null && musicService.isPlaying()) {
                    int pos = musicService.getCurrentPosition();
                    seekBar.setProgress(pos);
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(updateSeekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private void loadPlaylistDetails() {
        executor.execute(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                String token = sessionManager.fetchToken();

                var request = DaztlServiceOuterClass.PlaylistDetailRequest.newBuilder()
                        .setToken(token)
                        .setPlaylistId(playlistId)
                        .build();

                var response = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel())
                        .getPlaylistDetail(request);

                if (response.getStatus().equals("success")) {
                    playlistSongs.clear();
                    for (var sr : response.getSongsList()) {
                        Song song = new Song();
                        song.id = sr.getId();
                        song.title = sr.getTitle();
                        song.artistName = sr.getArtist();
                        song.audioUrl = sr.getAudioUrl();
                        song.coverUrl = sr.getCoverUrl();
                        song.releaseDate = sr.getReleaseDate();
                        playlistSongs.add(song);
                    }

                    runOnUiThread(() -> {
                        playlistSongsAdapter.setSongs(playlistSongs);
                        tvSongCount.setText(getString(R.string.song_count, playlistSongs.size()));

                        if (bound && musicService != null) {
                            musicService.setPlaylist(playlistSongs);
                        }
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error: " + response.getMessage(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error cargando playlist", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadAllSongs() {
        executor.execute(() -> {
            try {
                var response = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel())
                        .listSongs(DaztlServiceOuterClass.Empty.newBuilder().build());

                allSongs.clear();
                for (var sr : response.getSongsList()) {
                    Song song = new Song();
                    song.id = sr.getId();
                    song.title = sr.getTitle();
                    song.artistName = sr.getArtist();
                    song.audioUrl = sr.getAudioUrl();
                    song.setFullCoverUrl(sr.getCoverUrl());
                    song.releaseDate = sr.getReleaseDate();
                    allSongs.add(song);
                }

                runOnUiThread(() -> {
                    availableSongsAdapter.setSongs(allSongs);
                    filterAvailableSongs(""); // Mostrar todas inicialmente
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error cargando canciones", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void filterAvailableSongs(String query) {
        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            boolean isInPlaylist = false;
            for (Song playlistSong : playlistSongs) {
                if (playlistSong.id == song.id) {
                    isInPlaylist = true;
                    break;
                }
            }

            if (!isInPlaylist &&
                    (song.title.toLowerCase().contains(query.toLowerCase()) ||
                            song.artistName.toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(song);
            }
        }
        availableSongsAdapter.setSongs(filtered);
    }

    @Override
    public void onAddButtonClick(Song song) {
        addSongToPlaylist(song);
    }

    private void addSongToPlaylist(Song song) {
        executor.execute(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                String token = sessionManager.fetchToken();

                var request = DaztlServiceOuterClass.AddSongToPlaylistRequest.newBuilder()
                        .setToken(token)
                        .setPlaylistId(playlistId)
                        .setSongId(song.id)
                        .build();

                var response = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel())
                        .addSongToPlaylist(request);

                runOnUiThread(() -> {
                    if (response.getStatus().equals("success")) {
                        Toast.makeText(this,
                                "Canción agregada: " + song.title, Toast.LENGTH_SHORT).show();
                        loadPlaylistDetails();
                        filterAvailableSongs("");
                    } else {
                        Toast.makeText(this,
                                "Error: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error agregando canción", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}