package com.wtbruh.fakelauncher.ui.fragment.player;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import com.tencent.mmkv.MMKV;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.constants.SettingsConstants;
import com.wtbruh.fakelauncher.service.MusicService;
import com.wtbruh.fakelauncher.service.NotificationListenerService;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MusicPlayerFragment extends BaseFragment {
    private static final String TAG = MusicPlayerFragment.class.getSimpleName();

    public static final String MIME_AUDIO = "audio/";

    private MediaController mController;
    private ListenableFuture<MediaController> mControllerFuture;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mProgressUpdater;

    // UI
    private ImageView mAlbumArt;
    private TextView mTrackTitle, mTrackArtist, mAlbum;
    private TextView mTrackProgress, mTrackStatus;
    private ImageView mBtnPrev, mBtnPlayPause, mBtnNext;

    private List<MediaItem> mPlaylist;
    private int mCurrentIndex = -1;
    /** Whether the current controller belongs to an external app rather than MusicService */
    private boolean mIsExternalController = false;

    public MusicPlayerFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_player, container, false);

        mAlbumArt = view.findViewById(R.id.albumArt);
        mTrackTitle = view.findViewById(R.id.trackTitle);
        mTrackArtist = view.findViewById(R.id.trackArtist);
        mAlbum = view.findViewById(R.id.album);
        mTrackProgress = view.findViewById(R.id.trackProgress);
        mTrackStatus = view.findViewById(R.id.trackStatus);

        // Touch control buttons
        mBtnPrev = view.findViewById(R.id.btnPrev);
        mBtnPlayPause = view.findViewById(R.id.btnPlayPause);
        mBtnNext = view.findViewById(R.id.btnNext);

        mBtnPrev.setOnClickListener(v -> {
            if (mController != null) mController.seekToPreviousMediaItem();
        });

        mBtnPlayPause.setOnClickListener(v -> {
            togglePlayPause();
        });

        mBtnNext.setOnClickListener(v -> {
            if (mController != null) mController.seekToNextMediaItem();
        });

        // Set initial footer
        setFooterBar(
                L_EMPTY,
                C_PLAY,
                R_DEFAULT
        );

        // Don't show touchable control buttons on feature phone UI
        if (UIHelper.getCurrentUIType(requireContext()).equals(UIHelper.STYLE_PHONE)) {
            view.findViewById(R.id.controlButtons).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        connectToService();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopProgressUpdater();
        releaseController();
    }

    @Override
    public void onResume() {
        updateTrackInfo();
        super.onResume();
    }

    // ─── Button Handling (hardware keys) ──────────────────────

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mController == null) return false;

        // CENTER button → Play/Pause
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                togglePlayPause();
                return true;
            }
            // Previous track
            case KeyEvent.KEYCODE_DPAD_LEFT -> {
                mController.seekToPreviousMediaItem();
                return true;
            }
            // Next track
            case KeyEvent.KEYCODE_DPAD_RIGHT -> {
                mController.seekToNextMediaItem();
                return true;
            }
        }
        return false;
    }

    private void togglePlayPause() {
        if (mController == null) return;
        if (mController.isPlaying()) {
            mController.pause();
            setFooterBar(C_PLAY);
            mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
        } else {
            mController.play();
            setFooterBar(C_PAUSE);
            mBtnPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    // ─── MediaController Connection ───────────────────────────

    /**
     * Scans all active media sessions. Connects to a playing external one if found;
     * otherwise falls back to our own MusicService.
     */
    private void connectToService() {
        MediaSessionManager msm = (MediaSessionManager)
                requireContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
        ComponentName nlComponent = new ComponentName(
                requireContext(),
                NotificationListenerService.class);

        List<android.media.session.MediaController> sessions;
        try {
            sessions = msm.getActiveSessions(nlComponent);
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to list active sessions; using own service", e);
            connectOwnService();
            return;
        }

        for (android.media.session.MediaController pc : sessions) {
            PlaybackState ps = pc.getPlaybackState();
            if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                // Skip our own MusicService — we only care about *other* apps
                if (requireContext().getPackageName().equals(pc.getPackageName())) continue;

                Log.i(TAG, "Found playing external session: " + pc.getPackageName());
                connectToExternal(pc.getSessionToken());
                return;
            }
        }

        Log.i(TAG, "No external session is playing; connecting own service");
        connectOwnService();
    }

    private void connectOwnService() {
        mIsExternalController = false;
        SessionToken token = new SessionToken(
                requireContext(),
                new ComponentName(requireContext(), MusicService.class));

        mControllerFuture = new MediaController.Builder(requireContext(), token).buildAsync();
        mControllerFuture.addListener(() -> {
            try {
                mController = mControllerFuture.get();
                mHandler.post(() -> onConnected());
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to own MusicService", e);
                mHandler.post(() -> updateStatus(getString(R.string.music_connection_failed)));
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void connectToExternal(android.media.session.MediaSession.Token platformToken) {
        mIsExternalController = true;
        // Convert platform token to media3 SessionToken (async, per official demo)
        ListenableFuture<SessionToken> tokenFuture =
                SessionToken.createSessionToken(requireContext(), platformToken);
        tokenFuture.addListener(() -> {
            try {
                SessionToken media3Token = tokenFuture.get();
                mControllerFuture = new MediaController.Builder(requireContext(), media3Token).buildAsync();
                mControllerFuture.addListener(() -> {
                    try {
                        mController = mControllerFuture.get();
                        mHandler.post(this::onConnected);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to connect to external session, falling back", e);
                        mHandler.post(this::connectOwnService);
                    }
                }, Executors.newSingleThreadExecutor());
            } catch (Exception e) {
                Log.e(TAG, "Failed to create SessionToken from platform token, falling back", e);
                connectOwnService();
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void onConnected() {
        mController.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                updatePlaybackUI();
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateTrackInfo();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButton();
            }
        });

        // Only load our own playlist when connected to MusicService.
        // External controllers manage their own media items.
        if (!mIsExternalController) {
            loadMediaItems();
        }

        // Always refresh UI with the current playback state
        updateTrackInfo();
        updatePlayPauseButton();

        // Start progress updates
        startProgressUpdater();
    }

    private void releaseController() {
        if (mController != null) {
            MediaController.releaseFuture(mControllerFuture);
            mController = null;
        }
    }

    private void loadMediaItems() {
        // Never load our own playlist into an external controller
        if (mIsExternalController) return;

        mHandler.post(() -> {
            if (mController == null || mController.isPlaying() || mController.getMediaItemCount() > 0) return;

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String type = sp.getString(SettingsConstants.PREF_MUSIC_ACCESS_TYPE, getString(R.string.pref_music_access_type_default));

            if (type.equals("custom_dir")) {
                MMKV kv = MMKV.defaultMMKV();
                String mediaUri = kv.decodeString(SettingsConstants.PREF_MUSIC_ACCESS_SAF, "");
                ArrayList<Uri> mMusicUriList = new ArrayList<>();
                if (!mediaUri.isEmpty()) {
                    DocumentFile dir = DocumentFile.fromTreeUri(requireContext(), Uri.parse(mediaUri));
                    if (dir != null && dir.exists() && dir.isDirectory()) {
                        DocumentFile[] files = dir.listFiles();
                        for (DocumentFile file : files) {
                            Uri fileUri = file.getUri();
                            String mimeType = file.getType();
                            if (mimeType != null && mimeType.startsWith(MIME_AUDIO))
                                mMusicUriList.add(fileUri);
                        }
                    }
                    if (!mMusicUriList.isEmpty()) {
                        // Extract metadata in background — MediaMetadataRetriever does I/O
                        final ArrayList<Uri> uriList = new ArrayList<>(mMusicUriList);
                        final android.content.Context ctx = requireContext();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            mPlaylist = buildPlaylist(ctx, uriList);
                            mHandler.post(() -> {
                                if (mController != null) {
                                    mController.setMediaItems(mPlaylist);
                                    mController.prepare();
                                }
                            });
                        });
                    }
                }
            } else if (type.equals("auto_search")) {
                final android.content.Context ctx = requireContext();
                Executors.newSingleThreadExecutor().execute(() -> {
                    ArrayList<Uri> uris = new ArrayList<>();
                    ContentResolver resolver = ctx.getContentResolver();

                    String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
                    String sortOrder = MediaStore.Audio.Media.DISPLAY_NAME + " ASC";

                    try (Cursor cursor = resolver.query(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.Audio.Media._ID},
                            selection,
                            null,
                            sortOrder)) {

                        if (cursor != null && cursor.moveToFirst()) {
                            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                            do {
                                long id = cursor.getLong(idCol);
                                Uri contentUri = ContentUris.withAppendedId(
                                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                                uris.add(contentUri);
                            } while (cursor.moveToNext());
                        }
                    } catch (SecurityException e) {
                        Log.w(TAG, "MediaStore query denied (missing READ_MEDIA_AUDIO?)", e);
                    } catch (Exception e) {
                        Log.e(TAG, "MediaStore query failed", e);
                    }

                    if (!uris.isEmpty()) {
                        mPlaylist = buildPlaylist(ctx, uris);
                        mHandler.post(() -> {
                            if (mController != null) {
                                mController.setMediaItems(mPlaylist);
                                mController.prepare();
                            }
                        });
                    }
                });
            }
        });

    }

    /**
     * Builds a playlist from URIs, extracting embedded MP3 metadata (title, artist,
     * album, album art) via MediaMetadataRetriever. Runs on a background thread.
     */
    private List<MediaItem> buildPlaylist(android.content.Context ctx, List<Uri> uris) {
        List<MediaItem> playlist = new ArrayList<>();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        for (Uri uri : uris) {
            try {
                mmr.setDataSource(ctx, uri);

                String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

                // Fallback to filename when the file has no title tag
                if (title == null || title.isEmpty()) {
                    title = uri.getLastPathSegment();
                }

                MediaMetadata.Builder mmBuilder = new MediaMetadata.Builder()
                        .setTitle(title);
                if (artist != null && !artist.isEmpty()) {
                    mmBuilder.setArtist(artist);
                }
                if (album != null && !album.isEmpty()) {
                    mmBuilder.setAlbumTitle(album);
                }

                // Embedded album art — save to cache so artworkUri is set for Glide
                byte[] artBytes = mmr.getEmbeddedPicture();
                if (artBytes != null && artBytes.length > 0) {
                    Uri artCacheUri = saveArtworkToCache(ctx, uri, artBytes);
                    if (artCacheUri != null) {
                        mmBuilder.setArtworkUri(artCacheUri);
                    }
                }

                MediaItem item = new MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(mmBuilder.build())
                        .build();
                playlist.add(item);
            } catch (Exception e) {
                Log.w(TAG, "Failed to read metadata, using filename fallback: " + uri, e);
                // If metadata extraction fails entirely, still add the item with filename
                MediaItem item = new MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(new MediaMetadata.Builder()
                                .setTitle(uri.getLastPathSegment())
                                .build())
                        .build();
                playlist.add(item);
            }
        }
        try {
            mmr.release();
        } catch (Exception ignored) {
        }
        return playlist;
    }

    /**
     * Saves embedded album art bytes to a cache file and returns its Uri.
     * The filename is derived from the source track Uri to avoid duplicates.
     */
    @Nullable
    private Uri saveArtworkToCache(android.content.Context ctx, Uri trackUri, byte[] data) {
        File cacheDir = new File(ctx.getCacheDir(), "album_art");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Log.w(TAG, "Failed to create album art cache directory");
            return null;
        }
        // Use hash of track URI so the same track always hits the same cache file
        String filename = Integer.toHexString(trackUri.hashCode()) + ".jpg";
        File artFile = new File(cacheDir, filename);
        if (!artFile.exists()) {
            try (FileOutputStream fos = new FileOutputStream(artFile)) {
                fos.write(data);
                fos.flush();
            } catch (IOException e) {
                Log.w(TAG, "Failed to write album art cache file", e);
                return null;
            }
        }
        return Uri.fromFile(artFile);
    }

    // ─── UI Updates ───────────────────────────────────────────

    private void updateTrackInfo() {
        Log.d(TAG, "updateTrackInfo called");
        if (mController == null) return;

        MediaItem currentItem = mController.getCurrentMediaItem();
        if (currentItem == null && mPlaylist != null && !mPlaylist.isEmpty()) {
            // Auto-play first track if nothing is playing
            mController.setMediaItems(mPlaylist);
            mController.prepare();
            mController.play();
            return;
        }

        if (currentItem == null) {
            mHandler.post(() -> {
                mTrackTitle.setText(R.string.music_no_track);
                mTrackArtist.setText("");
                mAlbum.setText("");
                mAlbumArt.setImageResource(R.drawable.ic_music_note);
                mTrackStatus.setText(R.string.music_no_track);
            });
            return;
        }

        MediaMetadata metadata = currentItem.mediaMetadata;
        String title = metadata.title != null ? metadata.title.toString() : getString(R.string.music_unknown_title);
        String artist = metadata.artist != null ? metadata.artist.toString() : getString(R.string.music_unknown_artist);
        String album = metadata.albumTitle != null ? metadata.albumTitle.toString() : "";
        Uri artUri = metadata.artworkUri;
        byte[] artData = metadata.artworkData;

        // Find current index
        if (mPlaylist != null) {
            for (int i = 0; i < mPlaylist.size(); i++) {
                if (mPlaylist.get(i).mediaId.equals(currentItem.mediaId)) {
                    mCurrentIndex = i;
                    break;
                }
            }
        }

        mHandler.post(() -> {
            mTrackTitle.setText(title);
            mTrackArtist.setText(artist);
            mAlbum.setText(album);

            // Load album art with Glide — try URI first, then raw bytes, then fallback
            if (artUri != null) {
                Glide.with(MusicPlayerFragment.this)
                        .load(artUri)
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(mAlbumArt);
            } else if (artData != null && artData.length > 0) {
                Glide.with(MusicPlayerFragment.this)
                        .load(artData)
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(mAlbumArt);
            } else {
                mAlbumArt.setImageResource(R.drawable.ic_music_note);
            }

            updatePlaybackUI();
        });
    }

    private void updatePlaybackUI() {
        mHandler.post(() -> {
            if (mController == null) return;
            int state = mController.getPlaybackState();
            if (state == Player.STATE_BUFFERING) {
                mTrackStatus.setText(R.string.music_status_buffering);
            } else {
                mTrackStatus.setText("");
            }
        });
    }

    private void updatePlayPauseButton() {
        mHandler.post(() -> {
            if (mController != null && mController.isPlaying()) {
                setFooterBar(C_PAUSE);
                mBtnPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                setFooterBar(C_PLAY);
                mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            }
        });
    }

    private void updateStatus(String status) {
        mHandler.post(() -> mTrackStatus.setText(status));
    }

    // ─── Progress Bar ─────────────────────────────────────────

    private void startProgressUpdater() {
        mProgressUpdater = new Runnable() {
            @Override
            public void run() {
                if (mController != null && mController.isPlaying()) {
                    long position = mController.getCurrentPosition();
                    long duration = mController.getDuration();
                    mTrackProgress.setText(formatTime(position, duration));
                }
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.post(mProgressUpdater);
    }

    private void stopProgressUpdater() {
        if (mProgressUpdater != null) {
            mHandler.removeCallbacks(mProgressUpdater);
            mProgressUpdater = null;
        }
    }

    private String formatTime(long positionMs, long durationMs) {
        if (durationMs <= 0) return "--:-- / --:--";
        return String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d",
                (positionMs / 1000) / 60, (positionMs / 1000) % 60,
                (durationMs / 1000) / 60, (durationMs / 1000) % 60);
    }
}
