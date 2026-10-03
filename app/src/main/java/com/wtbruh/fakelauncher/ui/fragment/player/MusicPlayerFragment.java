package com.wtbruh.fakelauncher.ui.fragment.player;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import com.wtbruh.fakelauncher.BuildConfig;
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
    private TextView mBigPlay;
    private ImageView mTimerIcon;

    private List<MediaItem> mPlaylist;
    private int mCurrentIndex = -1;
    private volatile boolean mPlaylistLoading = false; // 列表重建中：快速播放会与 setMediaItems 冲突崩溃
    private static volatile List<MediaItem> sCachedPlaylist; // 进程内缓存：伪装界面内重复进入音乐不再重新扫描
    private static volatile String sCachedSourceKey;
    /** 定时关闭倒计时任务 */
    private Runnable mTimerRunnable;
    /** 定时关闭截止时间（epoch ms），0 = 未设置 */
    private long mTimerEnd = 0;
    /** Whether the current controller belongs to an external app rather than MusicService */
    private boolean mIsExternalController = false;
    /** 本地维护的播放状态：上移/切歌后恢复播放用（isPlaying() 依赖会话缓存，上移瞬间可能拿 false） */
    private boolean mLocalPlaying = false;

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

        // 中央大播放/暂停按钮（功能机风格，纯视觉展示——不设点击监听，避免抢按键焦点；操作走物理中键）
        mBigPlay = view.findViewById(R.id.bigPlay);
        mTimerIcon = view.findViewById(R.id.timerIcon);
        updateBigPlayButton();
        updateTimerIcon();

        // Set initial footer（中键隐藏文字，避免与中央大按钮重复；物理中键仍可播放/暂停）
        setFooterBar(
                L_OPTION,
                C_EMPTY,
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
        restoreTimer();
        if (mController == null) {
            connectToService();
        } else {
            // 复用已有连接（如从播放列表界面返回），恢复进度刷新与播放状态
            startProgressUpdater();
            updateTrackInfo();
            updatePlayPauseButton();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // 只停止进度刷新，不释放 controller：
        // 打开播放列表界面后返回时仍需立即播放指定歌曲
        stopProgressUpdater();
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
        // 左软键：选项菜单（列表/模式/歌单/定时）
        case KeyEvent.KEYCODE_MENU -> {
            showPlayerOptions();
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

    // ─── 选项菜单：列表 / 模式 / 定时 ──────────────────────

    private void showPlayerOptions() {
        String[] opts = {
                getString(R.string.music_option_list),
                getString(R.string.music_option_mode),
                getString(R.string.music_option_timer)
        };
        requireSubActivity().showOptionMenu(opts, (keyCode, event, position, tv) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                int sel = position;
                // 先关闭选项菜单，再执行动作，避免菜单残留遮挡新界面
                requireSubActivity().closeOptionMenu();
                switch (sel) {
                    case 0 -> openPlaylist();       // 列表
                    case 1 -> showModeOptions();    // 模式
                    case 2 -> showTimerDialog();    // 定时
                }
            }
            return true;
        });
    }

    /** 播放模式：列表循环 / 单曲循环 */
    private void showModeOptions() {
        String[] opts = {
                getString(R.string.music_mode_list_loop),
                getString(R.string.music_mode_single_loop)
        };
        requireSubActivity().showOptionMenu(opts, (keyCode, event, position, tv) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                int mode = position == 0 ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_ONE;
                if (mController != null) mController.setRepeatMode(mode);
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit().putInt("music_repeat_mode", mode).apply();
                requireSubActivity().closeOptionMenu();
            }
            return true;
        });
    }

    private void applyRepeatMode() {
        try {
            int mode = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt("music_repeat_mode", Player.REPEAT_MODE_ALL);
            if (mController != null) mController.setRepeatMode(mode);
        } catch (Exception e) {
            Log.w(TAG, "applyRepeatMode failed", e);
        }
    }

    /** 打开播放列表界面：显示所有歌曲，高亮当前播放歌曲 */
    private void openPlaylist() {
        List<String> titles = new ArrayList<>();
        List<String> uris = new ArrayList<>();
        if (mPlaylist != null) {
            for (MediaItem mi : mPlaylist) {
                titles.add(mi.mediaMetadata.title != null ? mi.mediaMetadata.title.toString() : "");
                uris.add(mi.mediaId);
            }
        }
        PlaylistFragment f = PlaylistFragment.newInstance(
                titles.toArray(new String[0]), uris.toArray(new String[0]), mCurrentIndex,
                index -> {
                    if (mController != null && mPlaylist != null && index >= 0 && index < mPlaylist.size()) {
                        // 选中的正是当前播放歌曲且正在播放 → 暂停，留在列表
                        if (index == mCurrentIndex && mLocalPlaying) {
                            mController.pause();
                            setFooterBar(C_EMPTY);
                            return;
                        }
                        // setMediaItems 异步：延迟再 seek+play，避免播错/卡住
                        final int playIndex = index;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                if (mController != null) {
                                    mController.seekToDefaultPosition(playIndex);
                                    mController.play();
                                }
                            } catch (Exception e) { Log.w(TAG, "play select err", e); }
                        }, 500);
                        setFooterBar(C_EMPTY);
                        // 播放后返回音乐播放器界面
                        requireActivity().onBackPressed();
                    }
                },
                (newTitles, newUris) -> {
                    // 列表内上移/下移后：按新顺序同步播放列表，当前播放曲目跟随
                    try {
                        if (mPlaylist == null || newUris == null) return;
                        java.util.Map<String, MediaItem> byId = new java.util.HashMap<>();
                        for (MediaItem mi : mPlaylist) byId.put(mi.mediaId, mi);
                        java.util.List<MediaItem> ordered = new java.util.ArrayList<>();
                        for (String id : newUris) {
                            MediaItem mi = byId.get(id);
                            if (mi != null) ordered.add(mi);
                        }
                        if (ordered.size() != mPlaylist.size()) return; // 数据异常不覆盖
                        String curId = (mCurrentIndex >= 0 && mCurrentIndex < mPlaylist.size())
                                ? mPlaylist.get(mCurrentIndex).mediaId : null;
                        mPlaylist = ordered;
                        if (mController != null) {
                            mController.setMediaItems(ordered);
                            mController.prepare();
                        }
                        if (curId != null) {
                            for (int i = 0; i < ordered.size(); i++) {
                                if (ordered.get(i).mediaId != null && ordered.get(i).mediaId.equals(curId)) {
                                    mCurrentIndex = i;
                                    break;
                                }
                            }
                        }
                        // setMediaItems 异步：延迟等播放器列表就绪后再定位到新索引并恢复播放状态
                        final int target = mCurrentIndex;
                        final boolean resume = mLocalPlaying;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                if (mController != null) {
                                    mController.seekToDefaultPosition(target);
                                    if (resume) mController.play();
                                }
                            } catch (Exception e) { Log.w(TAG, "resume after reorder err", e); }
                        }, 500);
                    } catch (Exception e) {
                        Log.w(TAG, "order sync err", e);
                    }
                });
        requireSubActivity().fragmentStarter(f);
    }

    /** 定时关闭弹窗：未设置→输入分钟；已设置→显示倒计时+取消 */
    private void showTimerDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).create();
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.timer_dialog, null);
        TextView input = v.findViewById(R.id.timerInput);
        TextView unit = v.findViewById(R.id.timerUnit);
        TextView cancel = v.findViewById(R.id.timerCancel);
        TextView confirm = v.findViewById(R.id.timerConfirm);
        cancel.setText(getString(R.string.music_timer_cancel));
        confirm.setText(getString(R.string.music_timer_confirm));
        dialog.setView(v);
        dialog.setCancelable(false);

        boolean hasTimer = mTimerEnd > System.currentTimeMillis();

        if (hasTimer) {
            // 倒计时模式：标题不变，显示倒计时；下方两个按钮：取消（取消定时）/ 返回（仅关闭弹窗，保留定时）
            unit.setVisibility(View.GONE);
            cancel.setText(getString(R.string.music_timer_cancel));
            confirm.setText(getString(R.string.common_rightButton)); // 返回
            cancel.setVisibility(View.VISIBLE);
            confirm.setVisibility(View.VISIBLE);
            input.setText(formatCountdown(mTimerEnd - System.currentTimeMillis()));
            final boolean[] selCancel = {true};
            Runnable refreshSel = () -> {
                cancel.setSelected(selCancel[0]);
                confirm.setSelected(!selCancel[0]);
            };
            refreshSel.run();
            cancel.setOnClickListener(vv -> {
                cancelTimer();
                dialog.dismiss();
            });
            confirm.setOnClickListener(vv -> dialog.dismiss()); // 返回：保留定时
            dialog.setOnKeyListener((d, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        selCancel[0] = !selCancel[0];
                        refreshSel.run();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_ENTER) {
                        if (selCancel[0]) cancelTimer();
                        // 延迟关闭：让本按键的 UP 先被弹窗窗口消费，防止冒泡到播放器误切换播放/暂停
                        new android.os.Handler(Looper.getMainLooper()).postDelayed(d::dismiss, 150);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_MENU
                            || keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
                        // 左软键=左下“返回”：仅关闭弹窗，保留定时
                        new android.os.Handler(Looper.getMainLooper()).postDelayed(d::dismiss, 150);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                        cancelTimer(); // 右软键=右下“取消”：取消定时并关闭
                        new android.os.Handler(Looper.getMainLooper()).postDelayed(d::dismiss, 150);
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                        || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                        || keyCode == KeyEvent.KEYCODE_ENTER
                        || keyCode == KeyEvent.KEYCODE_MENU
                        || keyCode == KeyEvent.KEYCODE_SOFT_LEFT
                        || keyCode == KeyEvent.KEYCODE_BACK) {
                    return true; // UP 也消费，防冒泡
                }
                return false;
            });
        } else {
            // 输入模式：输入框 + min + 确定 / 取消
            input.setText("0");
            unit.setVisibility(View.VISIBLE);
            confirm.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            Runnable onConfirm = () -> {
                int minutes = 0;
                try {
                    minutes = Integer.parseInt(sb.toString());
                } catch (Exception ignored) { }
                if (minutes > 0) {
                    scheduleTimer(minutes);
                    UIHelper.showCustomDialog(requireContext(), R.string.music_timer_set, null);
                }
                // 延迟关闭：防止确认键 UP 冒泡到播放器误切换播放/暂停
                new android.os.Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 150);
            };
            confirm.setOnClickListener(vv -> onConfirm.run());
            cancel.setOnClickListener(vv -> dialog.dismiss());
            dialog.setOnKeyListener((d, keyCode, event) -> {
                boolean isActionKey = (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
                        || keyCode == KeyEvent.KEYCODE_DEL
                        || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                        || keyCode == KeyEvent.KEYCODE_ENTER
                        || keyCode == KeyEvent.KEYCODE_MENU
                        || keyCode == KeyEvent.KEYCODE_SOFT_LEFT
                        || keyCode == KeyEvent.KEYCODE_BACK;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                        if (sb.length() < 4) {
                            sb.append((char) ('0' + keyCode - KeyEvent.KEYCODE_0));
                            input.setText(sb.toString());
                        }
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                        if (sb.length() > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                            input.setText(sb.length() == 0 ? "0" : sb.toString());
                        }
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                            || keyCode == KeyEvent.KEYCODE_ENTER) {
                        onConfirm.run();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_MENU
                            || keyCode == KeyEvent.KEYCODE_SOFT_LEFT) {
                        // 左软键=左下“确定”：保存并关闭
                        onConfirm.run();
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                        // 右软键=右下“取消”：关闭弹窗
                        new android.os.Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 150);
                        return true;
                    }
                }
                // UP 也消费已处理过的按键，防止冒泡触发播放器 toggle
                return isActionKey;
            });
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private String formatCountdown(long remain) {
        long sec = Math.max(0, remain / 1000);
        return String.format(Locale.CHINA, "%02d:%02d", sec / 60, sec % 60);
    }

    /** 设置定时：到点暂停播放，持久化截止时间 */
    private void scheduleTimer(int minutes) {
        mTimerEnd = System.currentTimeMillis() + minutes * 60L * 1000;
        if (mTimerRunnable != null) mHandler.removeCallbacks(mTimerRunnable);
        mTimerRunnable = () -> {
            if (mController != null) mController.pause();
            setFooterBar(C_EMPTY);
            mTimerEnd = 0;
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit().remove("music_timer_end").apply();
            updateTimerIcon();
        };
        mHandler.postDelayed(mTimerRunnable, minutes * 60L * 1000);
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit().putLong("music_timer_end", mTimerEnd).apply();
        updateTimerIcon();
    }

    /** 取消定时 */
    private void cancelTimer() {
        if (mTimerRunnable != null) mHandler.removeCallbacks(mTimerRunnable);
        mTimerRunnable = null;
        mTimerEnd = 0;
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit().remove("music_timer_end").apply();
        updateTimerIcon();
    }

    /** 恢复跨界面/重建前的定时任务 */
    private void restoreTimer() {
        try {
            long end = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getLong("music_timer_end", 0);
            if (end > System.currentTimeMillis()) {
                mTimerEnd = end;
                long remain = end - System.currentTimeMillis();
                if (mTimerRunnable != null) mHandler.removeCallbacks(mTimerRunnable);
                mTimerRunnable = () -> {
                    if (mController != null) mController.pause();
                    setFooterBar(C_EMPTY);
                    mTimerEnd = 0;
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit().remove("music_timer_end").apply();
                    updateTimerIcon();
                };
                mHandler.postDelayed(mTimerRunnable, remain);
                updateTimerIcon();
            } else if (end != 0) {
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit().remove("music_timer_end").apply();
                updateTimerIcon();
            }
        } catch (Exception e) {
            Log.w(TAG, "restoreTimer failed", e);
        }
    }

    private void togglePlayPause() {
        if (mController == null) return;
        try {
            if (mController.isPlaying()) {
                mController.pause();
                setFooterBar(C_EMPTY);
                mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            } else {
                // 列表未就绪（如刚进界面歌曲正在重新加载）时快速按播放会与 setMediaItems 冲突崩溃，先提示等待
                if (mPlaylistLoading) {
                    updateStatus(getString(R.string.music_loading));
                    return;
                }
                mController.play();
                setFooterBar(C_EMPTY);
                mBtnPlayPause.setImageResource(R.drawable.ic_pause);
            }
            updateBigPlayButton();
        } catch (Exception e) {
            // 播放/暂停异常（Android 13 媒体权限/会话状态问题）绝不能触发崩溃重启，吞掉并提示
            Log.e(TAG, "togglePlayPause error", e);
            updateStatus(getString(R.string.music_connection_failed));
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
                mLocalPlaying = isPlaying;
                updatePlayPauseButton();
            }
        });

        // Only load our own playlist when connected to MusicService.
        // External controllers manage their own media items.
        if (!mIsExternalController) {
            loadMediaItems();
        }

        // Apply remembered repeat mode
        applyRepeatMode();
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
            if (!isAdded()) return; // 界面已切换时不再加载/设置列表，避免 requireContext 崩溃
            // 只要当前未在播放就重新加载，保证新设置的目录/新歌曲立即生效
            if (mController == null || mController.isPlaying()) return;
            // 播放器已有列表（暂停/退出后再进）：保留当前曲目与进度，不再 setMediaItems 重置到开头；
            // 同时恢复列表缓存，供播放列表界面显示
            try {
                if (mController.getMediaItemCount() > 0) {
                    if (mPlaylist == null || mPlaylist.isEmpty()) {
                        mPlaylist = sCachedPlaylist;
                    }
                    return;
                }
            } catch (Exception ignored) {
            }
            mPlaylistLoading = true;

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String type = sp.getString(SettingsConstants.PREF_MUSIC_ACCESS_TYPE, getString(R.string.pref_music_access_type_default));

            // 进程内缓存：伪装界面内重复进入音乐直接复用列表，不重新扫描；退出伪装时清空
            final String sourceKey;
            if (type.equals("custom_dir")) {
                MMKV kv0 = MMKV.defaultMMKV();
                sourceKey = kv0.decodeString(SettingsConstants.PREF_MUSIC_ACCESS_SAF, "");
            } else if (type.equals("auto_search")) {
                sourceKey = "auto";
            } else {
                sourceKey = null;
            }
            if (sourceKey != null && sCachedPlaylist != null && sourceKey.equals(sCachedSourceKey)) {
                mPlaylist = sCachedPlaylist;
                try {
                    mController.setMediaItems(mPlaylist);
                    mController.prepare();
                } catch (Exception e) {
                    Log.e(TAG, "setMediaItems cached error", e);
                }
                mPlaylistLoading = false;
                mTrackProgress.setTranslationY(8f);
                return;
            }

            if (type.equals("custom_dir")) {
                MMKV kv = MMKV.defaultMMKV();
                String mediaUri = kv.decodeString(SettingsConstants.PREF_MUSIC_ACCESS_SAF, "");
                ArrayList<Uri> mMusicUriList = new ArrayList<>();
                Log.i(TAG, "DIAG custom_dir mediaUri='" + mediaUri + "'");
                if (!mediaUri.isEmpty()) {
                    DocumentFile dir = DocumentFile.fromTreeUri(requireContext(), Uri.parse(mediaUri));
                    Log.i(TAG, "DIAG dir null=" + (dir == null) + " exists=" + (dir != null && dir.exists())
                            + " isDir=" + (dir != null && dir.isDirectory()));
                    if (dir != null && dir.exists() && dir.isDirectory()) {
                        DocumentFile[] files = dir.listFiles();
                        Log.i(TAG, "DIAG files count=" + (files == null ? "null" : files.length));
                        if (files != null) for (DocumentFile file : files) {
                            Uri fileUri = file.getUri();
                            String mimeType = file.getType();
                            if (mimeType != null && mimeType.startsWith(MIME_AUDIO)) {
                                Log.i(TAG, "DIAG add audio mime=" + mimeType + " name=" + file.getName());
                                mMusicUriList.add(fileUri);
                            } else if (mimeType == null) {
                                // 部分文件管理器对音频文件返回 null MIME，按扩展名兜底识别
                                String name = file.getName();
                                if (name != null) {
                                    String lower = name.toLowerCase(Locale.ROOT);
                                    if (lower.endsWith(".mp3") || lower.endsWith(".wav")
                                            || lower.endsWith(".flac") || lower.endsWith(".m4a")
                                            || lower.endsWith(".ogg") || lower.endsWith(".aac")
                                            || lower.endsWith(".amr") || lower.endsWith(".wma")
                                            || lower.endsWith(".mid") || lower.endsWith(".midi")) {
                                        Log.i(TAG, "DIAG add by ext name=" + name);
                                        mMusicUriList.add(fileUri);
                                    } else {
                                        Log.i(TAG, "DIAG skip name=" + name + " mime=" + mimeType);
                                    }
                                }
                            }
                        }
                    }
                    Log.i(TAG, "DIAG musicUriList size=" + mMusicUriList.size());
                    if (!mMusicUriList.isEmpty()) {
                        // Extract metadata in background — MediaMetadataRetriever does I/O
                        final ArrayList<Uri> uriList = new ArrayList<>(mMusicUriList);
                        final android.content.Context ctx = requireContext();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            mPlaylist = buildPlaylist(ctx, uriList);
                            mHandler.post(() -> {
                                if (mController != null) {
                                    try {
                                        mController.setMediaItems(mPlaylist);
                                        mController.prepare();
                                    } catch (Exception e) {
                                        Log.e(TAG, "setMediaItems error", e);
                                    }
                                }
                                mPlaylistLoading = false;
                                mTrackProgress.setTranslationY(8f);
                                sCachedPlaylist = mPlaylist;
                                sCachedSourceKey = sourceKey;
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
                                try {
                                    mController.setMediaItems(mPlaylist);
                                    mController.prepare();
                                } catch (Exception e) {
                                    Log.e(TAG, "setMediaItems error", e);
                                }
                            }
                            sCachedPlaylist = mPlaylist;
                            sCachedSourceKey = sourceKey;
                            mPlaylistLoading = false;
                            mTrackProgress.setTranslationY(8f);
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
    private List<MediaItem> buildPlaylist(Context ctx, List<Uri> uris) {
        List<Uri> allUris = new ArrayList<>();

        // 用户目录歌曲优先显示，内置歌曲殿后
        allUris.addAll(uris);
        // 内置歌曲：正确的 raw resource URI 必须是 android.resource://包名/资源ID
        allUris.add(new Uri.Builder()
                .scheme("android.resource")
                .authority(BuildConfig.APPLICATION_ID)
                .path(String.valueOf(R.raw.haoyunlai))
                .build());
        allUris.add(new Uri.Builder()
                .scheme("android.resource")
                .authority(BuildConfig.APPLICATION_ID)
                .path(String.valueOf(R.raw.xilenianhua))
                .build());

        List<MediaItem> playlist = new ArrayList<>();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        for (Uri uri : allUris) {
            try {
                mmr.setDataSource(ctx, uri);

                String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

                // Fallback to real filename when the file has no title tag
                if (title == null || title.isEmpty()) {
                    String name = null;
                    try {
                        DocumentFile df = DocumentFile.fromSingleUri(ctx, uri);
                        if (df != null) name = df.getName();
                    } catch (Exception ignored) { }
                    title = (name != null && !name.isEmpty()) ? name : uri.getLastPathSegment();
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
                        .setMediaId(uri.toString())
                        .setUri(uri)
                        .setMediaMetadata(mmBuilder.build())
                        .build();
                playlist.add(item);
            } catch (Exception e) {
                Log.w(TAG, "Failed to read metadata, using filename fallback: " + uri, e);
                // If metadata extraction fails entirely, still add the item with real filename
                String fname = null;
                try {
                    DocumentFile df = DocumentFile.fromSingleUri(ctx, uri);
                    if (df != null) fname = df.getName();
                } catch (Exception ignored) { }
                MediaItem item = new MediaItem.Builder()
                        .setMediaId(uri.toString())
                        .setUri(uri)
                        .setMediaMetadata(new MediaMetadata.Builder()
                                .setTitle((fname != null && !fname.isEmpty()) ? fname : uri.getLastPathSegment())
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
            if (!isAdded()) return; // 播放瞬间 fragment 可能已分离（界面切换），跳过 UI 更新避免崩溃
            if (mController != null && mController.isPlaying()) {
                setFooterBar(C_EMPTY);
                mBtnPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                setFooterBar(C_EMPTY);
                mBtnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            }
            updateBigPlayButton();
        });
    }

    /** 中央大按钮文字：播放 / 暂停 随播放状态切换 */
    private void updateBigPlayButton() {
        mHandler.post(() -> {
            if (!isAdded() || mBigPlay == null) return;
            boolean playing = mController != null && mController.isPlaying();
            mBigPlay.setText(playing ? R.string.pause_centerButton : R.string.play_centerButton);
            // 播放/暂停统一为选中样式（功能机蓝底），风格一致
            mBigPlay.setSelected(true);
        });
    }

    /** 右上角定时图标：有未到期定时时显示 */
    private void updateTimerIcon() {
        mHandler.post(() -> {
            if (mTimerIcon == null) return;
            mTimerIcon.setVisibility(mTimerEnd > System.currentTimeMillis()
                    ? View.VISIBLE : View.GONE);
        });
    }

    /** 退出伪装时清空音乐缓存：下次进入伪装重新扫描曲目（可发现新增歌曲） */
    public static void clearCache() {
        sCachedPlaylist = null;
        sCachedSourceKey = null;
    }

    private void updateStatus(String status) {
        if (!isAdded()) return;
        mHandler.post(() -> mTrackStatus.setText(status));
    }

    // ─── Progress Bar ─────────────────────────────────────────

    private void startProgressUpdater() {
        mProgressUpdater = new Runnable() {
            @Override
            public void run() {
                if (mController != null) {
                    // 暂停/停止时也刷新时长：重进音乐界面（暂停状态）也应显示当前进度与总时长
                    try {
                        long position = mController.getCurrentPosition();
                        long duration = mController.getDuration();
                        if (duration > 0) {
                            mTrackProgress.setText(formatTime(position, duration));
                        }
                    } catch (Exception ignored) {
                    }
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
