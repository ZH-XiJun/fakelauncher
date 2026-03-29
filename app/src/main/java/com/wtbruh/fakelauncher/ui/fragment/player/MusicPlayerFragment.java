package com.wtbruh.fakelauncher.ui.fragment.player;

import android.content.Context;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import android.media.session.MediaController;

import com.google.common.util.concurrent.ListenableFuture;

import com.wtbruh.fakelauncher.ApplicationHelper;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.service.NotificationListenerService;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.MediaAppDetails;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerFragment extends BaseFragment {
    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;
    private ListenableFuture<MediaBrowser> mBrowserFuture;

    private final static String TAG = MusicPlayerFragment.class.getSimpleName();

    public MusicPlayerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init();
    }

    @Override
    public void onStart() {
        super.onStart();
        initMediaController();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_music_player, container, false);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    private void initMediaController() {
        mMediaController = null;
        MediaSessionManager mMediaSessionManager = (MediaSessionManager) requireContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
        List<MediaController> mediaControllers = mMediaSessionManager.getActiveSessions(
                ((ApplicationHelper) requireActivity().getApplication()).getComponentName(NotificationListenerService.class)
        );
        if (mediaControllers.size() == 1) {
            mMediaController = mediaControllers.get(0);
        } else {
            for (MediaController controller : mediaControllers) {
                Log.d(TAG, String.valueOf(controller.getPlaybackState()));
                PlaybackState ps = controller.getPlaybackState();
                if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                    mMediaController = controller;
                }
            }
        }
    }

}