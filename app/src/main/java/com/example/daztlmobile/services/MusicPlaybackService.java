package com.example.daztlmobile.services;


import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.example.daztlmobile.models.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicPlaybackService extends Service {

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();

    private List<Song> playlist = new ArrayList<>();
    private int currentIndex = -1;

    private final String ACTION_PLAYBACK_STATUS = "com.example.daztlmobile.PLAYBACK_STATUS";

    public class LocalBinder extends Binder {
        public MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnCompletionListener(mp -> {
            playNext();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            return false;
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setPlaylist(List<Song> songs) {
        this.playlist = songs;
    }

    public List<Song> getPlaylist() {
        return playlist;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }


    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public void playSong(int index) {
        if (index < 0 || index >= playlist.size()) return;

        currentIndex = index;
        Song song = playlist.get(index);

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(song.getFullAudioUrl());
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                notifyPrepared();
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface OnPlaybackPreparedListener {
        void onPrepared();
    }

    private OnPlaybackPreparedListener preparedListener;

    public void setOnPlaybackPreparedListener(OnPlaybackPreparedListener listener) {
        this.preparedListener = listener;
    }

    private void notifyPrepared() {
        if (preparedListener != null) {
            preparedListener.onPrepared();
        }
    }



    public void playNext() {
        if (currentIndex < playlist.size() - 1) {
            playSong(currentIndex + 1);
        } else {
            // Llegó al final
            stopPlayback();
        }
    }

    public void playPrev() {
        if (currentIndex > 0) {
            playSong(currentIndex - 1);
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            broadcastUpdate();
        }
    }

    public void resume() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            broadcastUpdate();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    private void stopPlayback() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        currentIndex = -1;
        broadcastUpdate();
    }

    private void broadcastUpdate() {
        Intent intent = new Intent(ACTION_PLAYBACK_STATUS);
        intent.putExtra("currentIndex", currentIndex);
        intent.putExtra("isPlaying", mediaPlayer.isPlaying());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
