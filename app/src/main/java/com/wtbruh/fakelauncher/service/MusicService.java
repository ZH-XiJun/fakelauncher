package com.wtbruh.fakelauncher.service;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * MediaLibraryService that owns the MediaSession and ExoPlayer.
 * Scans local audio files via MediaStore and exposes them as a browsable library.
 */
public class MusicService extends MediaSessionService {
    private static final String TAG = MusicService.class.getSimpleName();

    public static final String ROOT_ID = "root";

    private ExoPlayer mPlayer;
    private List<MediaItem> mPlaylist = new ArrayList<>();
    private boolean mPlaylistLoaded = false;
    private MediaSession mSession;
    // ─── Lifecycle ────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();

        mPlayer = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build();

        mPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    // Auto-play next when a track finishes naturally
                    if (mPlayer.hasNextMediaItem()) {
                        mPlayer.seekToNextMediaItem();
                        mPlayer.play();
                    }
                }
            }
        });

        mSession = new MediaSession.Builder(this, mPlayer).build();

        // Start scanning in background
        // Executors.newSingleThreadExecutor().execute(this::loadAudioFiles);
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mSession != null) mSession.release();
        if (mPlayer != null) mPlayer.release();
        super.onDestroy();
    }

    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mSession;
    }

    // ─── Library + Playback Callbacks ─────────────────────────


    // ─── Audio File Scanner ───────────────────────────────────
//
//    private void loadAudioFiles() {
//        String[] projection = {
//                MediaStore.Audio.Media._ID,
//                MediaStore.Audio.Media.TITLE,
//                MediaStore.Audio.Media.ARTIST,
//                MediaStore.Audio.Media.ALBUM,
//                MediaStore.Audio.Media.DURATION,
//                MediaStore.Audio.Media.DATA,
//                MediaStore.Audio.Media.ALBUM_ID,
//        };
//
//        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
//        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
//
//        try (Cursor cursor = getContentResolver().query(
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                projection, selection, null, sortOrder)) {
//
//            if (cursor == null || !cursor.moveToFirst()) {
//                Log.w(TAG, "No audio files found");
//                mPlaylistLoaded = true;
//                return;
//            }
//
//            List<MediaItem> items = new ArrayList<>();
//            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
//            int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
//            int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
//            int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
//            int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
//            int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
//
//            do {
//                long id = cursor.getLong(idCol);
//                String title = cursor.getString(titleCol);
//                String artist = cursor.getString(artistCol);
//                String album = cursor.getString(albumCol);
//                String path = cursor.getString(dataCol);
//                long albumId = cursor.getLong(albumIdCol);
//
//                if (title == null) title = "Unknown";
//                if (artist == null) artist = "Unknown Artist";
//
//                Uri artUri = ContentUris.withAppendedId(
//                        Uri.parse("content://media/external/audio/albumart"), albumId);
//
//                MediaMetadata metadata = new MediaMetadata.Builder()
//                        .setTitle(title)
//                        .setArtist(artist)
//                        .setAlbumTitle(album != null ? album : "Unknown Album")
//                        .setArtworkUri(artUri)
//                        .build();
//
//                MediaItem item = new MediaItem.Builder()
//                        .setMediaId(String.valueOf(id))
//                        .setMediaMetadata(metadata)
//                        .setUri(Uri.parse(path))
//                        .build();
//
//                items.add(item);
//            } while (cursor.moveToNext());
//
//            mPlaylist = items;
//            mPlaylistLoaded = true;
//            Log.d(TAG, "Loaded " + mPlaylist.size() + " audio files");
//            mSession.notifyChildrenChanged(ROOT_ID, mPlaylist.size(), null);
//
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to load audio files", e);
//            mPlaylistLoaded = true;
//        }
//    }
//
//    // ─── Playback Helpers ─────────────────────────────────────
//
//    /**
//     * Loads the full playlist into ExoPlayer and starts at the given index.
//     */
//    private void queuePlaylistAndPlay(int startIndex) {
//        if (mPlaylist.isEmpty()) return;
//        if (startIndex < 0 || startIndex >= mPlaylist.size()) startIndex = 0;
//
//        mPlayer.setMediaItems(mPlaylist, startIndex, 0);
//        mPlayer.prepare();
//        mPlayer.play();
//    }
}
