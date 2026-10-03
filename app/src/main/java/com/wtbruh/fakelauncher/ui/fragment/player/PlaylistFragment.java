package com.wtbruh.fakelauncher.ui.fragment.player;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 音乐播放列表界面
 * 底部三按钮：上移 / 播放·暂停 / 返回
 * 方向键上下循环选择，中键播放选中歌曲
 */
public class PlaylistFragment extends BaseFragment {

    public interface OnPlaySelected {
        void playAt(int index);
    }

    /** 歌曲顺序变化回调：上移/下移后通知外层同步播放列表顺序 */
    public interface OnOrderChanged {
        void onOrderChanged(String[] titles, String[] uris);
    }

    private static final String KEY_TITLES = "titles";
    private static final String KEY_URIS = "uris";
    private static final String KEY_CURRENT = "current";

    private String[] mTitles;
    private String[] mUris;
    private int mCurrent = -1;
    private int mPos = 0;
    private OnPlaySelected mOnPlay;
    private OnOrderChanged mOnOrderChanged;

    private RecyclerView mListView;
    private SingleTextviewAdapter mAdapter;

    public PlaylistFragment() {}

    public static PlaylistFragment newInstance(String[] titles, String[] uris, int currentIndex,
                                               OnPlaySelected onPlay) {
        return newInstance(titles, uris, currentIndex, onPlay, null);
    }

    public static PlaylistFragment newInstance(String[] titles, String[] uris, int currentIndex,
                                               OnPlaySelected onPlay, OnOrderChanged onOrderChanged) {
        PlaylistFragment f = new PlaylistFragment();
        Bundle b = new Bundle();
        b.putStringArray(KEY_TITLES, titles);
        b.putStringArray(KEY_URIS, uris);
        b.putInt(KEY_CURRENT, currentIndex);
        f.setArguments(b);
        f.mOnPlay = onPlay;
        f.mOnOrderChanged = onOrderChanged;
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_playlist, container, false);
        init();
        return rootView;
    }

    private void init() {
        Bundle args = getArguments();
        if (args != null) {
            mTitles = args.getStringArray(KEY_TITLES);
            mUris = args.getStringArray(KEY_URIS);
            mCurrent = args.getInt(KEY_CURRENT, -1);
        }
        if (mTitles == null || mTitles.length == 0) {
            mTitles = new String[]{getString(R.string.music_playlist_empty)};
        }
        mPos = (mCurrent >= 0 && mCurrent < mTitles.length) ? mCurrent : 0;

        mListView = findViewById(R.id.playlistList);
        mListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mListView.setFocusable(false);
        mListView.setItemAnimator(null);
        mListView.setOnTouchListener((v, e) -> true); // 禁触

        mAdapter = new SingleTextviewAdapter(new ArrayList<>(Arrays.asList(mTitles)));
        mAdapter.setSelectedMarquee(true); // 音乐列表：选中项滚动显示完整名称
        mListView.setAdapter(mAdapter);
        mAdapter.setSelectedPosition(mPos);
        mListView.scrollToPosition(mPos);

        setFooterBar(
                new String[]{LEFT_BTN, String.valueOf(R.string.music_btn_up)},
                new String[]{CENTER_BTN, String.valueOf(R.string.music_btn_play_pause)},
                R_DEFAULT
        );
    }

    private void movePosUp() {
        int max = mAdapter.getItemCount() - 1;
        if (max < 0) return;
        mPos = mPos <= 0 ? max : mPos - 1;
        mAdapter.setSelectedPosition(mPos);
        mListView.scrollToPosition(mPos);
    }

    /** 上移选中歌曲：把选中项在列表中上移一位（改变播放顺序，高亮跟随） */
    private void moveSongUp() {
        if (mAdapter == null) return;
        int count = mAdapter.getItemCount();
        if (count < 2 || mPos <= 0) return;
        int target = mPos - 1;
        if (mTitles != null && mUris != null) {
            String t = mTitles[mPos]; mTitles[mPos] = mTitles[target]; mTitles[target] = t;
            String u = mUris[mPos]; mUris[mPos] = mUris[target]; mUris[target] = u;
        }
        mAdapter.swapItems(mPos, target);
        mPos = target;
        mAdapter.setSelectedPosition(mPos);
        mListView.scrollToPosition(mPos);
        if (mOnOrderChanged != null) mOnOrderChanged.onOrderChanged(mTitles, mUris);
    }

    private void movePosDown() {
        int max = mAdapter.getItemCount() - 1;
        if (max < 0) return;
        mPos = mPos >= max ? 0 : mPos + 1;
        mAdapter.setSelectedPosition(mPos);
        mListView.scrollToPosition(mPos);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> {
                movePosUp();
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN -> {
                movePosDown();
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (mAdapter != null && mAdapter.getItemCount() > 0 && mOnPlay != null) {
                    mOnPlay.playAt(mPos);
                }
                return true;
            }
            // 左软键：上移选中歌曲（改变播放顺序）
            case KeyEvent.KEYCODE_MENU -> {
                moveSongUp();
                return true;
            }
            case KeyEvent.KEYCODE_BACK -> {
                return false;
            }
        }
        return false;
    }
}
