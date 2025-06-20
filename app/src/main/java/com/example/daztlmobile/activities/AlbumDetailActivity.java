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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.SongAdapter;
import com.example.daztlmobile.models.Album;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.services.MusicPlaybackService;
import com.example.daztlmobile.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class AlbumDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private ImageView ivAlbumCover;
    private TextView tvAlbumTitle;
    private TextView tvArtistName;
    private TextView tvReleaseDate;
    private RecyclerView rvAlbumSongs;
    private SongAdapter songAdapter;
    private ExecutorService executor = Executors.newFixedThreadPool(2);

    private ImageButton btnPlayPause, btnNext, btnPrev;
    private SeekBar seekBar;

    private MusicPlaybackService musicService;
    private boolean bound = false;
    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;

    private List<Song> albumSongs = new ArrayList();
    private int albumId;
    private String albumTitle;

    private static String currentFilesURL = "http://10.0.2.2:8000/media/";

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

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MusicPlaybackService.ACTION_PLAYBACK_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(playbackReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver wasn't registered, ignore
            Log.e("AlbumDetail", "Receiver not registered", e);
        }

        if (bound && musicService != null && !musicService.isPlaying()) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        albumId = getIntent().getIntExtra("album_id", -1);
        String albumTitle = getIntent().getStringExtra("album_title");
        String artistName = getIntent().getStringExtra("album_artist");
        String coverUrl = getIntent().getStringExtra("album_cover_url");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initViews();
        setupAdapters();
        setupPlaybackControls();

        tvAlbumTitle.setText(albumTitle);
        tvArtistName.setText(artistName);

        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .into(ivAlbumCover);
        }

        loadAlbumDetails();
    }

    private void initViews() {
        ivAlbumCover = findViewById(R.id.ivAlbumCover);
        tvAlbumTitle = findViewById(R.id.tvAlbumTitle);
        tvArtistName = findViewById(R.id.tvArtistName);
        tvReleaseDate = findViewById(R.id.tvReleaseDate);
        rvAlbumSongs = findViewById(R.id.rvAlbumSongs);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        seekBar = findViewById(R.id.seekBar);
    }

    private void setupAdapters() {
        rvAlbumSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(this);
        rvAlbumSongs.setAdapter(songAdapter);
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
                    if (musicService.getCurrentSong() == null && !albumSongs.isEmpty()) {
                        musicService.setPlaylist(albumSongs);
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
            if (!bound) return;
            musicService.playNext();
            updateUIState();
            startSeekBarUpdate();
        });

        btnPrev.setOnClickListener(v -> {
            if (!bound) return;
            musicService.playPrev();
            updateUIState();
            startSeekBarUpdate();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!bound) return;
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

    private void loadAlbumDetails() {
        executor.execute(() -> {
            try {
                Log.d("AlbumDetail", "Loading details for album ID: " + albumId);

                var channel = GrpcClient.getChannel();
                var stub = MusicServiceGrpc.newBlockingStub(channel);

                var request = DaztlServiceOuterClass.AlbumDetailRequest.newBuilder()
                        .setAlbumId(albumId)
                        .build();

                var response = stub.getAlbumDetail(request);

                if (!"success".equals(response.getStatus())) {
                    throw new Exception(response.getMessage());
                }

                Album album = new Album();
                album.id = response.getId();
                album.title = response.getTitle();
                album.artistName = response.getArtistName();
                album.coverUrl = currentFilesURL+response.getCoverUrl();

                List<Song> songs = new ArrayList<>();
                for (var sr : response.getSongsList()) {
                    Song s = new Song();
                    s.id = sr.getId();
                    s.title = sr.getTitle();
                    s.artistName = sr.getArtist();
                    s.audioUrl = currentFilesURL+sr.getAudioUrl();
                    s.coverUrl = currentFilesURL+sr.getCoverUrl();
                    s.releaseDate = sr.getReleaseDate();
                    songs.add(s);
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    tvAlbumTitle.setText(album.title);
                    tvArtistName.setText(album.artistName);

                    albumSongs = songs;
                    songAdapter.setSongs(albumSongs);

                    if (!songs.isEmpty() && songs.get(0).releaseDate != null) {
                        tvReleaseDate.setText(songs.get(0).releaseDate);
                    }
                });

            } catch (Exception e) {
                Log.e("AlbumDetail", "Error loading album details", e);
                runOnUiThread(() -> {
                    Toast.makeText(AlbumDetailActivity.this, "Error cargando álbum: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    tvAlbumTitle.setText(getIntent().getStringExtra("album_title"));
                    tvArtistName.setText(getIntent().getStringExtra("album_artist"));

                    String coverUrl = getIntent().getStringExtra("album_cover_url");
                    if (coverUrl != null && !coverUrl.isEmpty()) {
                        Glide.with(AlbumDetailActivity.this)
                                .load(currentFilesURL+coverUrl)
                                .into(ivAlbumCover);
                    }
                });
            }
        });
    }

    @Override
    public void onSongClick(Song song) {
        if (!bound || musicService == null) {
            return;
        }

        if (!albumSongs.contains(song)) {
            Log.e("Playback", "Canción no encontrada en el álbum");
            return;
        }

        musicService.setOnPlaybackPreparedListener(() -> runOnUiThread(() -> {
            updateUIState();
            startSeekBarUpdate();
        }));

        musicService.setPlaylist(albumSongs);
        int index = albumSongs.indexOf(song);
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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.LocalBinder binder = (MusicPlaybackService.LocalBinder) service;
            musicService = binder.getService();
            bound = true;

            musicService.setOnPlaybackPreparedListener(() -> {
                runOnUiThread(() -> {
                    updateUIState();
                    startSeekBarUpdate();
                });
            });

            if (musicService.isPlaying()) {
                updateUIState();
                startSeekBarUpdate();
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
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}