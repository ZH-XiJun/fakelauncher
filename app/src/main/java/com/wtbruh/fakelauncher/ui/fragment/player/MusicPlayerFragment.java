package com.wtbruh.fakelauncher.ui.fragment.player;

import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.service.MusicService;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MusicPlayerFragment extends BaseFragment {
    private static final String TAG = "MusicPlayerFragment";

    private MediaController mController;
    private ListenableFuture<MediaController> mControllerFuture;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mProgressUpdater;

    // UI
    private ImageView mAlbumArt;
    private TextView mTrackTitle, mTrackArtist, mAlbum;
    private TextView mTrackProgress, mTrackStatus, mTrackCount;
    private ImageView mBtnPrev, mBtnPlayPause, mBtnNext;

    private List<MediaItem> mPlaylist;
    private int mCurrentIndex = -1;

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
        mTrackCount = view.findViewById(R.id.trackCount);

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

    // ─── Button Handling (hardware keys) ──────────────────────

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mController == null) return false;

        // CENTER button → Play/Pause
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            togglePlayPause();
            return true;
        }
        // Previous track
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mController.seekToPreviousMediaItem();
            return true;
        }
        // Next track
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mController.seekToNextMediaItem();
            return true;
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

    private void connectToService() {
        SessionToken token = new SessionToken(
                requireContext(),
                new android.content.ComponentName(requireContext(), MusicService.class)
        );

        mControllerFuture = new MediaController.Builder(requireContext(), token).buildAsync();
        mControllerFuture.addListener(() -> {
            try {
                mController = mControllerFuture.get();
                onConnected();
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to MusicService", e);
                updateStatus(getString(R.string.connection_failed));
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

        // Set initial footer
        setFooterBar(
                R_EMPTY.length > 0 ? R_EMPTY : L_DEFAULT,
                C_PLAY,
                R_DEFAULT
        );

        // Load library
        // loadLibrary();

        // Start progress updates
        startProgressUpdater();
    }

    private void releaseController() {
        if (mController != null) {
            MediaController.releaseFuture(mControllerFuture);
            mController = null;
        }
    }


    // ─── UI Updates ───────────────────────────────────────────

    private void updateTrackInfo() {
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
                mTrackTitle.setText(R.string.no_track);
                mTrackArtist.setText("");
                mAlbum.setText("");
                mAlbumArt.setImageResource(R.drawable.ic_music_note);
                mTrackCount.setText("");
                mTrackStatus.setText(R.string.no_track);
            });
            return;
        }

        MediaMetadata metadata = currentItem.mediaMetadata;
        String title = metadata.title != null ? metadata.title.toString() : getString(R.string.unknown_title);
        String artist = metadata.artist != null ? metadata.artist.toString() : getString(R.string.unknown_artist);
        String album = metadata.albumTitle != null ? metadata.albumTitle.toString() : "";
        Uri artUri = metadata.artworkUri;

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

            // Load album art with Glide
            if (artUri != null) {
                Glide.with(MusicPlayerFragment.this)
                        .load(artUri)
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(mAlbumArt);
            } else {
                mAlbumArt.setImageResource(R.drawable.ic_music_note);
            }

            // Track count
            if (mPlaylist != null && mCurrentIndex >= 0) {
                mTrackCount.setText(getString(R.string.track_count_fmt, mCurrentIndex + 1, mPlaylist.size()));
            }

            updatePlaybackUI();
        });
    }

    private void updatePlaybackUI() {
        mHandler.post(() -> {
            if (mController == null) return;
            int state = mController.getPlaybackState();
            switch (state) {
                case Player.STATE_IDLE:
                    mTrackStatus.setText(R.string.status_idle);
                    break;
                case Player.STATE_BUFFERING:
                    mTrackStatus.setText(R.string.status_buffering);
                    break;
                case Player.STATE_READY:
                    mTrackStatus.setText(mController.isPlaying() ? R.string.status_playing : R.string.status_paused);
                    break;
                case Player.STATE_ENDED:
                    mTrackStatus.setText(R.string.status_ended);
                    break;
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
