package com.example.daztlmobile.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.SongAdapter;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.services.MusicPlaybackService;
import com.example.daztlmobile.utils.AuthUtils;
import com.example.daztlmobile.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.example.daztlmobile.network.GrpcClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class HomeActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private EditText etSearch;
    private RecyclerView rvContent;
    private SongAdapter adapter;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageButton btnPlayPause, btnNext, btnPrev, btnCreatePlaylist, btnLibrary;
    private SeekBar seekBar;

    private MusicPlaybackService musicService;
    private boolean bound = false;

    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;

    private List<Song> songList = new ArrayList<>();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.LocalBinder binder = (MusicPlaybackService.LocalBinder) service;
            musicService = binder.getService();
            bound = true;

            musicService.setOnPlaybackPreparedListener(() -> runOnUiThread(() -> {
                updateUIState();
                startSeekBarUpdate();
            }));

            musicService.setPlaylist(songList);

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
        btnCreatePlaylist = findViewById(R.id.btnCreatePlaylist);

        rvContent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this);
        rvContent.setAdapter(adapter);

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        btnLibrary = findViewById(R.id.btnLibrary);
        btnLibrary.setOnClickListener(v -> {
            Intent intent = new Intent(this, LibraryActivity.class);
            startActivity(intent);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                if (query.length() > 2) {
                    searchSongs(query);
                } else if (query.isEmpty()) {
                    loadAllSongs();
                }
            }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (!bound) return;

            if (musicService.isPlaying()) {
                musicService.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
                stopSeekBarUpdate();
            } else {
                musicService.resume();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                startSeekBarUpdate();
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!bound) return;
                if (fromUser) {
                    musicService.seekTo(progress);
                    updateUIState(); // Actualizar para reflejar el cambio inmediato
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        loadAllSongs();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
        stopSeekBarUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void updateUIState() {
        if (!bound) return;
        Song current = musicService.getCurrentSong();
        if (current != null) {
            int pos = musicService.getCurrentPosition();
            int dur = musicService.getDuration();

            seekBar.setMax(dur > 0 ? dur : 0);
            seekBar.setProgress(pos);

            btnPlayPause.setImageResource(musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        } else {
            seekBar.setProgress(0);
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate(); // Para evitar múltiples callbacks
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

                    if (bound && musicService != null) {
                        musicService.setPlaylist(songList);
                    }
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
                SessionManager sessionManager = new SessionManager(this);
                var channel = GrpcClient.getChannel();
                var accessToken = sessionManager.fetchToken();

                Metadata metadata = new Metadata();
                Metadata.Key<String> AUTHORIZATION_KEY =
                        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
                metadata.put(AUTHORIZATION_KEY, "Bearer " + accessToken);

                var stub = MusicServiceGrpc.newBlockingStub(channel)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

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
                    songList = songs;
                    adapter.setSongs(songList);

                    if (bound && musicService != null) {
                        musicService.setPlaylist(songList);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error en búsqueda", Toast.LENGTH_SHORT).show());
            }
        });
    }


    @Override
    public void onSongClick(Song song) {
        if (!bound) return;
        int index = songList.indexOf(song);
        if (index != -1) {
            musicService.playSong(index);
        }
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        builder.setView(dialogView);

        EditText etPlaylistName = dialogView.findViewById(R.id.etPlaylistName);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String name = etPlaylistName.getText().toString().trim();
            if (name.isEmpty()) {
                etPlaylistName.setError("Ingresa un nombre");
            } else {
                dialog.dismiss();
                createPlaylist(name);
            }
        });
    }

    private void createPlaylist(String name) {
        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.fetchToken();

        DaztlServiceOuterClass.CreatePlaylistRequest request = DaztlServiceOuterClass.CreatePlaylistRequest.newBuilder()
                .setToken(token)
                .setName(name)
                .build();

        new Thread(() -> {
            try {
                var stub = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel());
                var response = stub.createPlaylist(request);

                runOnUiThread(() -> {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    if ("success".equalsIgnoreCase(response.getStatus())) {
                        int playlistId = extractIdFromMessage(response.getMessage());
                        Intent intent = new Intent(this, PlaylistDetailActivity.class);
                        intent.putExtra("playlist_name", name);
                        intent.putExtra("playlist_id", playlistId);
                        startActivity(intent);
                    } else {
                        refreshTokenAndRetryCreatePlaylist(name);
                    }
                });

            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Token is expired")) {
                    refreshTokenAndRetryCreatePlaylist(name);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Error al crear playlist: " + errorMessage, Toast.LENGTH_LONG).show());
                }
            }
        }).start();
    }

    private void refreshTokenAndRetryCreatePlaylist(String name) {
        new Thread(() -> {
            String newToken = AuthUtils.refreshAccessToken(HomeActivity.this);
            if (newToken != null) {
                SessionManager sm = new SessionManager(HomeActivity.this);
                sm.saveToken(newToken);

                runOnUiThread(() -> createPlaylist(name));
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, "Sesión expirada. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
                    SessionManager sm = new SessionManager(HomeActivity.this);
                    sm.clear();
                    Intent intent = new Intent(HomeActivity.this, SignupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
            }
        }).start();
    }

    private int extractIdFromMessage(String message) {
        try {
            String[] parts = message.split("\\D+");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isEmpty()) {
                    return Integer.parseInt(parts[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
