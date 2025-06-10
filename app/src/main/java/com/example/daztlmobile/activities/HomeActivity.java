package com.example.daztlmobile.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.daztlmobile.R;
import com.example.daztlmobile.adapters.SongAdapter;
import com.example.daztlmobile.models.RecommendedSongsResponse;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.network.ApiClient;
import com.example.daztlmobile.network.ApiService;
import com.example.daztlmobile.utils.SessionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private EditText etSearch;
    private RecyclerView rvContent;
    private SongAdapter adapter;
    private ApiService api;
    private SessionManager session;
    private ImageButton btnPlayPause, btnNext, btnPrev;
    private Button btnLogout, btnMyPlaylists;

    private Song currentSong = null;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        session = new SessionManager(this);
        api = ApiClient.getClient(this).create(ApiService.class);

        etSearch = findViewById(R.id.etSearch);
        rvContent = findViewById(R.id.rvContent);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnLogout = findViewById(R.id.btnLogout);
        btnMyPlaylists = findViewById(R.id.btnMyPlaylists);

        rvContent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this); // Le pasamos el listener
        rvContent.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) {
                    searchSongs(s.toString());
                }
            }
        });

        loadRecommendedSongs();

        btnPlayPause.setOnClickListener(v -> {
            if (currentSong != null) {
                isPlaying = !isPlaying;
                Toast.makeText(this, isPlaying ? "Reproduciendo" : "Pausado", Toast.LENGTH_SHORT).show();
                btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            } else {
                Toast.makeText(this, "Selecciona una canción", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(v -> Toast.makeText(this, "Siguiente canción (no implementado)", Toast.LENGTH_SHORT).show());
        btnPrev.setOnClickListener(v -> Toast.makeText(this, "Canción anterior (no implementado)", Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            session.clear();
            finish();
        });

        btnMyPlaylists.setOnClickListener(v -> {
            Toast.makeText(this, "Ir a biblioteca (no implementado aún)", Toast.LENGTH_SHORT).show();
        });
    }

    private void searchSongs(String query) {
         /*api.searchSongs(query).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(@NonNull Call<List<Song>> call, @NonNull Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setSongs(response.body());
                } else {
                    Toast.makeText(HomeActivity.this, "No se encontraron resultados", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Song>> call, @NonNull Throwable t) {
                Toast.makeText(HomeActivity.this, "Fallo al buscar", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    private void loadRecommendedSongs() {
        /*api.getRecommended().enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(@NonNull Call<List<Song>> call, @NonNull Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setSongs(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Song>> call, @NonNull Throwable t) {
                Toast.makeText(HomeActivity.this, "Fallo al cargar contenido", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    @Override
    public void onSongClick(Song song) {
        currentSong = song;
        isPlaying = true;
        btnPlayPause.setImageResource(R.drawable.ic_pause);
        Toast.makeText(this, "Reproduciendo: " + song.title, Toast.LENGTH_SHORT).show();

    }

    /*private void fetchRecommendedSongs() {
        String token = "Bearer " + SessionManager.getInstance(this).getAuthToken();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<RecommendedSongsResponse> call = apiService.getRecommendedSongs(token);
        call.enqueue(new Callback<RecommendedSongsResponse>() {
            @Override
            public void onResponse(Call<RecommendedSongsResponse> call, Response<RecommendedSongsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Song> recommended = response.body().getRecommended();
                    // Puedes usar un nuevo RecyclerView o el existente
                    SongAdapter adapter = new SongAdapter(recommended, HomeActivity.this);
                    recyclerViewRecommended.setAdapter(adapter); // asegúrate de tener este RecyclerView en el layout
                } else {
                    Toast.makeText(HomeActivity.this, "No se pudo obtener recomendaciones", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RecommendedSongsResponse> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }*/

}
