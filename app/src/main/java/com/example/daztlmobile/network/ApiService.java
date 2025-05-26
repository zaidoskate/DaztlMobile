package com.example.daztlmobile.network;

import com.example.daztlmobile.models.RegisterRequest;
import com.example.daztlmobile.models.LoginRequest;
import com.example.daztlmobile.models.LoginResponse;
import com.example.daztlmobile.models.UserResponse;
import com.example.daztlmobile.models.Song;
import com.example.daztlmobile.models.Album;
import com.example.daztlmobile.models.ArtistProfile;
import com.example.daztlmobile.models.Playlist;
import com.example.daztlmobile.models.LikeStatusResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

public interface ApiService {
    // — CU-01 / CU-02 Authentication
    @POST("register/")
    Call<UserResponse> register(@Body RegisterRequest body);

    @POST("login/")
    Call<LoginResponse> login(@Body LoginRequest body);

    // — CU-03 / CU-04 Buscar contenido
    @GET("songs/")
    Call<List<Song>> getSongs(@Query("q") String query);

    @GET("songs/{id}/")
    Call<Song> getSongById(@Path("id") int songId);

    @GET("albums/")
    Call<List<Album>> getAlbums(@Query("q") String query);

    @GET("artists/")
    Call<List<ArtistProfile>> getArtists(@Query("q") String query);

    // — CU-05/06/07 Playlists (si luego lo implementas)
    @GET("playlists/")
    Call<List<Playlist>> getPlaylists();

    @POST("playlists/")
    Call<Playlist> createPlaylist(@Body Playlist body);

    // — CU-13 Like/Unlike artista
    @POST("artists/{id}/like/")
    Call<ResponseBody> likeArtist(@Path("id") int artistId);

    @DELETE("artists/{id}/unlike/")
    Call<ResponseBody> unlikeArtist(@Path("id") int artistId);

    @GET("artists/{id}/like/status/")
    Call<LikeStatusResponse> isArtistLiked(@Path("id") int artistId);

    // Agrega aquí más endpoints según tus CU futuros...
}
