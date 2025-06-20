package com.example.daztlmobile.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.AlbumAdapter;
import com.example.daztlmobile.adapters.ArtistAdapter;
import com.example.daztlmobile.adapters.PlaylistAdapter;
import com.example.daztlmobile.adapters.SongAdapter;
import com.example.daztlmobile.models.Album;
import com.example.daztlmobile.models.Artist;
import com.example.daztlmobile.models.Playlist;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.services.MusicPlaybackService;
import com.example.daztlmobile.utils.AuthUtils;
import com.example.daztlmobile.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class HomeActivity extends AppCompatActivity implements
        SongAdapter.OnSongClickListener,
        PlaylistAdapter.OnPlaylistClickListener,
        AlbumAdapter.OnAlbumClickListener,
        ArtistAdapter.OnArtistClickListener {

    private EditText etSearch;
    private RecyclerView rvSongs, rvPlaylists, rvAlbums, rvArtists;
    private SongAdapter songAdapter;
    private PlaylistAdapter playlistAdapter;
    private AlbumAdapter albumAdapter;
    private ArtistAdapter artistAdapter;
    private ExecutorService executor = Executors.newFixedThreadPool(4);

    private ImageButton btnPlayPause, btnNext, btnPrev, btnCreatePlaylist;
    private SeekBar seekBar;

    private MusicPlaybackService musicService;
    private boolean bound = false;

    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable, searchRunnable;

    private List<Song> songList = new ArrayList<>();
    private List<Playlist> playlistList = new ArrayList<>();
    private List<Album> albumList = new ArrayList<>();
    private List<Artist> artistList = new ArrayList<>();
    private AlertDialog searchDialog;
    private RecyclerView rvSearchResults;
    private SongAdapter searchResultsAdapter;
    private List<Song> searchResults = new ArrayList<>();
    private ImageView dialogCoverPreview;

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
    protected void onPause() {
        super.onPause();
        unregisterReceiver(playbackReceiver);
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
        initViews();
        setupAdapters();
        setupListeners();
        setupSearchDialog();
        loadAllContent();
    }


    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        rvSongs = findViewById(R.id.rvSongs);
        rvPlaylists = findViewById(R.id.rvPlaylists);
        rvAlbums = findViewById(R.id.rvAlbums);
        rvArtists = findViewById(R.id.rvArtists);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        seekBar = findViewById(R.id.seekBar);
        btnCreatePlaylist = findViewById(R.id.btnCreatePlaylist);
    }

    private void setupAdapters() {
        rvSongs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvArtists.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        songAdapter = new SongAdapter(this);
        playlistAdapter = new PlaylistAdapter(this);
        albumAdapter = new AlbumAdapter(this);
        artistAdapter = new ArtistAdapter(this);
        rvSongs.setAdapter(songAdapter);
        rvPlaylists.setAdapter(playlistAdapter);
        rvAlbums.setAdapter(albumAdapter);
        rvArtists.setAdapter(artistAdapter);
    }

    private void setupListeners() {
        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                if (!query.isEmpty()) {
                    searchRunnable = () -> {
                        if (searchDialog != null) {
                            searchDialog.show();
                        }
                        searchSongs(query);
                    };
                    handler.postDelayed(searchRunnable, 800);
                } else {
                    if (searchDialog != null && searchDialog.isShowing()) {
                        searchDialog.dismiss();
                    }
                    loadAllContent();
                }
            }
        });

        setupPlaybackControls();
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
                        if (musicService.getCurrentSong() == null && !songList.isEmpty()) {
                            musicService.setPlaylist(songList);
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!bound) return;
                if (fromUser) {
                    musicService.seekTo(progress);
                    updateUIState();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadAllContent() {
        loadSongs();
        loadPlaylists();
        loadAlbums();
        loadArtists();
    }

    private void loadSongs() {
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
                    s.audioUrl = currentFilesURL+sr.getAudioUrl();
                    s.coverUrl = currentFilesURL+sr.getCoverUrl();
                    s.releaseDate = sr.getReleaseDate();
                    songs.add(s);
                }

                runOnUiThread(() -> {
                    songList = songs;
                    songAdapter.setSongs(songList);

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

    private void loadPlaylists() {
        executor.execute(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                var channel = GrpcClient.getChannel();
                var accessToken = sessionManager.fetchToken();

                var request = DaztlServiceOuterClass.PlaylistListRequest.newBuilder()
                        .setToken(accessToken)
                        .build();

                var stub = MusicServiceGrpc.newBlockingStub(channel);
                var resp = stub.listPlaylists(request);

                List<Playlist> playlists = new ArrayList<>();
                for (var pr : resp.getPlaylistsList()) {
                    Playlist p = new Playlist();
                    p.id = pr.getId();
                    p.name = pr.getName();
                    p.coverUrl = currentFilesURL+pr.getCoverUrl();

                    List<Song> songs = new ArrayList<>();
                    for (var sr : pr.getSongsList()) {
                        Song s = new Song();
                        s.id = sr.getId();
                        s.title = sr.getTitle();
                        s.artistName = sr.getArtist();
                        s.audioUrl = currentFilesURL+sr.getAudioUrl();
                        s.coverUrl = currentFilesURL+sr.getCoverUrl();
                        s.releaseDate = sr.getReleaseDate();
                        songs.add(s);
                    }
                    p.songs = songs;

                    playlists.add(p);
                }

                runOnUiThread(() -> {
                    playlistList = playlists;
                    playlistAdapter.setPlaylists(playlistList);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error cargando playlists", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadAlbums() {
        executor.execute(() -> {
            try {
                var channel = GrpcClient.getChannel();
                var stub = MusicServiceGrpc.newBlockingStub(channel);
                var resp = stub.listAlbums(DaztlServiceOuterClass.Empty.newBuilder().build());

                List<Album> albums = new ArrayList<>();
                for (var ar : resp.getAlbumsList()) {
                    Album a = new Album();
                    a.id = ar.getId();
                    a.title = ar.getTitle();
                    a.artistName = ar.getArtistName();
                    a.coverUrl = currentFilesURL+ar.getCoverUrl();
                    albums.add(a);
                }

                runOnUiThread(() -> {
                    albumList = albums;
                    albumAdapter.setAlbums(albumList);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error cargando álbumes", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadArtists() {
        executor.execute(() -> {
            try {
                var channel = GrpcClient.getChannel();
                var stub = MusicServiceGrpc.newBlockingStub(channel);
                var resp = stub.listArtists(DaztlServiceOuterClass.Empty.newBuilder().build());

                List<Artist> artists = new ArrayList<>();
                for (var ar : resp.getArtistsList()) {
                    Artist a = new Artist();
                    a.id = ar.getId();
                    a.name = ar.getName();
                    a.profilePicture = currentFilesURL+ar.getProfilePicture();
                    a.isLiked = false;
                    artists.add(a);
                }

                checkArtistLikes(artists);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error cargando artistas", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_results, null);
        builder.setView(dialogView);

        searchDialog = builder.create();
        searchDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        searchDialog.getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );

        searchDialog.setCanceledOnTouchOutside(true);
        searchDialog.setOnDismissListener(dialog -> clearSearch());

        rvSearchResults = dialogView.findViewById(R.id.rvSearchResults);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));

        searchResultsAdapter = new SongAdapter(this);
        rvSearchResults.setAdapter(searchResultsAdapter);

        Button btnCloseSearch = dialogView.findViewById(R.id.btnCloseSearch);
        btnCloseSearch.setOnClickListener(v -> searchDialog.dismiss());
    }

    private void showSearchResults(List<Song> results) {
        try {
            searchResults.clear();
            searchResults.addAll(results);

            if (searchResultsAdapter == null) {
                return;
            }

            searchResultsAdapter.setSongs(searchResults);

            if (searchDialog == null) {
                setupSearchDialog();
            }

            if (!searchDialog.isShowing()) {
                searchDialog.show();
            }
        } catch (Exception e) {
            Log.e("SearchUI", "Error en showSearchResults", e);
        }
    }

    private void clearSearch() {
        etSearch.setText("");
        searchResults.clear();
        searchResultsAdapter.setSongs(searchResults);
    }

    private void searchSongs(String query) {
        executor.execute(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                var channel = GrpcClient.getChannel();
                var accessToken = sessionManager.fetchToken();

                if (accessToken == null || accessToken.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show());
                    return;
                }

                Metadata metadata = new Metadata();
                Metadata.Key<String> AUTHORIZATION_KEY =
                        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
                metadata.put(AUTHORIZATION_KEY, "Bearer " + accessToken);

                var stub = MusicServiceGrpc.newBlockingStub(channel)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

                var req = DaztlServiceOuterClass.SearchRequest.newBuilder()
                        .setQuery(query)
                        .build();

                var resp = stub.searchSongs(req);
                List<Song> songs = new ArrayList<>();
                for (var sr : resp.getSongsList()) {
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
                    Log.d("SearchUI", "Ejecutando runOnUiThread");
                    if (isFinishing() || isDestroyed()) {
                        Log.d("SearchUI", "Activity no está disponible");
                        return;
                    }

                    if (songs.isEmpty()) {
                        Log.d("SearchUI", "Mostrando toast de no resultados");
                        Toast.makeText(this, "No se encontraron resultados", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("SearchUI", "Preparando para mostrar resultados: " + songs.size());
                        if (searchDialog == null) {
                            Log.d("SearchUI", "Creando diálogo de búsqueda");
                            setupSearchDialog();
                        }
                        showSearchResults(songs);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onSongClick(Song song) {
        if (!bound || musicService == null) {
            return;
        }

        List<Song> targetPlaylist;
        if (searchResults.contains(song)) {
            targetPlaylist = new ArrayList<>(searchResults);
            musicService.setPlaylist(targetPlaylist);
        } else if (songList.contains(song)) {
            targetPlaylist = new ArrayList<>(songList);
            musicService.setPlaylist(targetPlaylist);
        } else {
            Log.e("Playback", "Canción no encontrada en ninguna lista");
            return;
        }

        musicService.setOnPlaybackPreparedListener(() -> runOnUiThread(() -> {
            Log.d("Playback", "Preparación completada - actualizando UI");
            updateUIState();
            startSeekBarUpdate();
        }));

        int index = targetPlaylist.indexOf(song);
        musicService.playSong(index);

        runOnUiThread(() -> {
            updateUIState();
            startSeekBarUpdate();
        });

        if (searchDialog != null && searchDialog.isShowing()) {
            searchDialog.dismiss();
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("playlist_id", playlist.id);
        intent.putExtra("playlist_name", playlist.name);
        intent.putExtra("playlist_cover_url", playlist.getFullCoverUrl());
        startActivity(intent);
    }

    @Override
    public void onAlbumClick(Album album) {
        Intent intent = new Intent(this, AlbumDetailActivity.class);
        intent.putExtra("album_id", album.id);
        intent.putExtra("album_title", album.title);
        intent.putExtra("album_artist", album.artistName);
        intent.putExtra("album_cover_url", album.getFullCoverUrl());
        startActivity(intent);
    }

    @Override
    public void onArtistClick(Artist artist) {

    }

    @Override
    public void onLikeClick(Artist artist, int position) {
        toggleArtistLike(artist, position);
    }

    private void toggleArtistLike(Artist artist, int position) {
        executor.execute(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                String token = sessionManager.fetchToken();

                var request = DaztlServiceOuterClass.ArtistIdRequest.newBuilder()
                        .setArtistId(artist.id)
                        .setToken(token)
                        .build();

                var response = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel())
                        .likeArtist(request);

                runOnUiThread(() -> {
                    if (response.getStatus().equals("success")) {
                        // Actualizar el estado visual del like
                        boolean newLikeStatus = !artist.isLiked;
                        artistAdapter.updateArtistLikeStatus(position, newLikeStatus);

                        Toast.makeText(this,
                                newLikeStatus ? "Artista marcado como favorito" : "Artista removido de favoritos",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Error: " + response.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error al actualizar like", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void checkArtistLikes(List<Artist> artists) {
        executor.execute(() -> {
            try {
                SessionManager sessionManager = new SessionManager(this);
                String token = sessionManager.fetchToken();

                for (Artist artist : artists) {
                    var request = DaztlServiceOuterClass.ArtistIdRequest.newBuilder()
                            .setArtistId(artist.id)
                            .setToken(token)
                            .build();

                    var response = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel())
                            .isArtistLiked(request);

                    artist.isLiked = response.getIsLiked();
                }

                runOnUiThread(() -> artistAdapter.setArtists(artists));
            } catch (Exception e) {
                Log.e("ArtistLikes", "Error checking likes", e);
                // Mostramos los artistas aunque falle la verificación de likes
                runOnUiThread(() -> artistAdapter.setArtists(artists));
            }
        });
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

    private static final int PICK_IMAGE_REQUEST = 1001;
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        builder.setView(dialogView);

        EditText etPlaylistName = dialogView.findViewById(R.id.etPlaylistName);
        dialogCoverPreview = dialogView.findViewById(R.id.ivCoverPreview); // Guardamos la referencia
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        Button btnCreate = dialogView.findViewById(R.id.btnCreate);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Resetear la imagen seleccionada
        selectedImageUri = null;
        selectedImageBitmap = null;

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Seleccionar imagen"), PICK_IMAGE_REQUEST);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnCreate.setOnClickListener(v -> {
            String name = etPlaylistName.getText().toString().trim();
            if (name.isEmpty()) {
                etPlaylistName.setError("Ingresa un nombre");
            } else {
                dialog.dismiss();
                if (selectedImageBitmap != null) {
                    createPlaylistWithCover(name, selectedImageBitmap);
                } else {
                    createPlaylist(name, null);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);

                if (dialogCoverPreview != null && selectedImageBitmap != null) {
                    dialogCoverPreview.setImageBitmap(selectedImageBitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createPlaylistWithCover(String name, Bitmap coverBitmap) {
        // Comprimir la imagen a Base64
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        coverBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String coverBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

        createPlaylist(name, coverBase64);
    }

    private void createPlaylist(String name, String coverBase64) {
        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.fetchToken();

        DaztlServiceOuterClass.CreatePlaylistRequest.Builder requestBuilder =
                DaztlServiceOuterClass.CreatePlaylistRequest.newBuilder()
                        .setToken(token)
                        .setName(name);

        if (coverBase64 != null && !coverBase64.isEmpty()) {
            requestBuilder.setCoverUrl(coverBase64);
        }

        DaztlServiceOuterClass.CreatePlaylistRequest request = requestBuilder.build();

        new Thread(() -> {
            try {
                var stub = MusicServiceGrpc.newBlockingStub(GrpcClient.getChannel());
                var response = stub.createPlaylist(request);

                runOnUiThread(() -> {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    if ("success".equalsIgnoreCase(response.getStatus())) {
                        loadPlaylists();
                    } else {
                        refreshTokenAndRetryCreatePlaylist(name, coverBase64);
                    }
                });

            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Token is expired")) {
                    refreshTokenAndRetryCreatePlaylist(name, coverBase64);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Error al crear playlist: " + errorMessage, Toast.LENGTH_LONG).show());
                }
            }
        }).start();
    }

    private void refreshTokenAndRetryCreatePlaylist(String name, String coverBase64) {
        new Thread(() -> {
            String newToken = AuthUtils.refreshAccessToken(HomeActivity.this);
            if (newToken != null) {
                SessionManager sm = new SessionManager(HomeActivity.this);
                sm.saveToken(newToken);

                runOnUiThread(() -> {
                    if (coverBase64 != null) {
                        createPlaylist(name, coverBase64);
                    } else {
                        createPlaylist(name, null);
                    }
                });
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

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound && musicService != null && !musicService.isPlaying()) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
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