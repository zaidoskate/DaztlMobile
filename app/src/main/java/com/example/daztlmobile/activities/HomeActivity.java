package com.example.daztlmobile.activities;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.SongAdapter;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.utils.SessionManager;
import com.example.daztlmobile.utils.SimpleTextWatcher;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;

public class HomeActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private EditText etSearch;
    private RecyclerView rvContent;
    private SongAdapter adapter;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageButton btnPlayPause, btnNext, btnPrev;
    private SeekBar seekBar;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;
    private boolean isPrepared = false;
    private Song currentSong;
    private List<Song> songList = new ArrayList<>();
    private int currentSongIndex = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etSearch = findViewById(R.id.etSearch);
        rvContent = findViewById(R.id.rvContent);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        seekBar = findViewById(R.id.seekBar);

        rvContent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this);
        rvContent.setAdapter(adapter);

        loadAllSongs();

        etSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String q = s.toString();
                if (q.length() > 2) {
                    searchSongs(q);
                } else if (q.isEmpty()) {
                    loadAllSongs();
                }
            }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null && isPrepared) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    startSeekBarUpdate();
                }
            }
        });
        btnNext.setOnClickListener(v -> {
            if (songList != null && !songList.isEmpty() && currentSongIndex < songList.size() - 1) {
                currentSongIndex++;
                currentSong = songList.get(currentSongIndex);
                prepareAndPlay(currentSong.getFullAudioUrl());
            } else {
                Toast.makeText(this, "No hay siguiente canción", Toast.LENGTH_SHORT).show();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (songList != null && !songList.isEmpty()) {
                if (currentSongIndex > 0) {
                    currentSongIndex--;
                    currentSong = songList.get(currentSongIndex);
                    prepareAndPlay(currentSong.getFullAudioUrl());
                } else {
                    Toast.makeText(this, "No hay canción anterior", Toast.LENGTH_SHORT).show();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && isPrepared) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadAllSongs() {
        executor.execute(() -> {
            try {
                var channel = GrpcClient.getChannel();
                var stub = MusicServiceGrpc.newBlockingStub(channel);
                var resp = stub.listSongs(DaztlServiceOuterClass.Empty.newBuilder().build());
                List<Song> songs = new ArrayList<>();
                for (var sr : resp.getSongsList()) {
                    Song s = new Song();
                    s.id = sr.getId();
                    s.title = sr.getTitle();
                    s.artistName = sr.getArtist();
                    s.audioUrl = sr.getAudioUrl();
                    s.coverUrl = sr.getCoverUrl();
                    s.releaseDate = sr.getReleaseDate();
                    songs.add(s);
                }
                runOnUiThread(() -> {
                    songList = songs;
                    adapter.setSongs(songList);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error cargando canciones", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void searchSongs(String query) {
        executor.execute(() -> {
            try {
                var channel = GrpcClient.getChannel(); // No se cierra aquí
                var stub = daztl.MusicServiceGrpc.newBlockingStub(channel);
                var req = DaztlServiceOuterClass.SearchRequest.newBuilder().setQuery(query).build();
                var resp = stub.searchSongs(req);
                List<Song> songs = new ArrayList<>();
                for (var sr : resp.getSongsList()) {
                    Song s = new Song();
                    s.id = sr.getId();
                    s.title = sr.getTitle();
                    s.artistName = sr.getArtist();
                    s.audioUrl = sr.getAudioUrl();
                    s.coverUrl = sr.getCoverUrl();
                    s.releaseDate = sr.getReleaseDate();
                    songs.add(s);
                }
                runOnUiThread(() -> {
                    if (songs.isEmpty()) {
                        Toast.makeText(this, "No se encontraron resultados", Toast.LENGTH_SHORT).show();
                    }
                    runOnUiThread(() -> {
                        songList = songs;
                        adapter.setSongs(songList);
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error en búsqueda", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onSongClick(Song song) {
        int index = songList.indexOf(song);
        if (index != -1) {
            currentSongIndex = index;
            currentSong = songList.get(currentSongIndex);
            prepareAndPlay(currentSong.getFullAudioUrl());
        }
    }


    private void prepareAndPlay(String url) {
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        isPrepared = false;
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                seekBar.setMax(mediaPlayer.getDuration());
                mediaPlayer.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                startSeekBarUpdate();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                handler.removeCallbacks(updateSeekBar);
                if (songList != null && currentSongIndex < songList.size() - 1) {
                    currentSongIndex++;
                    currentSong = songList.get(currentSongIndex);
                    prepareAndPlay(currentSong.getFullAudioUrl());
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            });
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                runOnUiThread(() -> Toast.makeText(this, "Error MediaPlayer: " + what + ", extra: " + extra, Toast.LENGTH_LONG).show());
                return true;
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            runOnUiThread(() -> Toast.makeText(this, "Error al reproducir la canción", Toast.LENGTH_SHORT).show());
        }
    }

    private void startSeekBarUpdate() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(updateSeekBar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekBar);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        executor.shutdownNow();
        GrpcClient.shutdownChannel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_logout) {
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.clear();

            Intent intent = new Intent(this, SignupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
