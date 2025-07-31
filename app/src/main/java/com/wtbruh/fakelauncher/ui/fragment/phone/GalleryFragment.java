package com.wtbruh.fakelauncher.ui.fragment.phone;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.BaseAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Gallery 相册
 */
public class GalleryFragment extends BaseFragment {

    private final static String TAG = GalleryFragment.class.getSimpleName();
    public final static String MIME_IMAGE = "image/";
    public final static String MIME_VIDEO = "video/";

    private ImageView fullscreenView;
    private TextView textHint;
    private View videoLayout;
    private SurfaceView videoView;
    private RecyclerView galleryView;
    private ProgressBar bar;

    private MediaPlayer mediaPlayer = null;
    private GalleryAdapter adapter;
    private Timer timer;
    private List<HashMap<String, Uri>> mPhotoUriList;

    public GalleryFragment() {
        // Required empty public constructor
    }
    public static GalleryFragment newInstance() {
        return new GalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_gallery, container, false);
        init();
        return rootView;
    }

    private void init(){
        // 加载图片的时候左边的按键提示先别显示出来
        // Do not show left button hint when loading
        setFooterBar(SubActivity.L_EMPTY);
        // 绑定控件 View binding
        galleryView = rootView.findViewById(R.id.gallery);
        galleryView.setFocusable(false);
        galleryView.setItemAnimator(null);
        galleryView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        videoLayout = rootView.findViewById(R.id.video);
        bar = rootView.findViewById(R.id.videoProgressBar);

        fullscreenView = rootView.findViewById(R.id.fullscreenView);
        textHint = rootView.findViewById(R.id.gallery_textHint);
        videoView = rootView.findViewById(R.id.videoView);
        SurfaceHolder surfaceHolder = videoView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                mediaPlayer.setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });
        // 从SharedPreferences拿到用户给我们授权访问的目录的Uri
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String uriStr = sp.getString(SubSettingsFragment.PREF_GALLERY_ACCESS_URI, "");
        if (! uriStr.isEmpty()) {
            // 有东西，那就尝试读下照片
            // If URI is not empty, try to load the photos
            readAllPhotos(Uri.parse(uriStr));
        } else {
            // 如果拿到个空的，说明用户根本没授权啊，要么就是用户趁我不注意偷偷改了SharedPreference
            // If URI is empty, show "no photo" text.
            noPhoto();
        }
    }

    private void noPhoto() {
        // 显示“无照片”对应的TextView，其它的都隐藏
        // Hide all view instead of "No photo"
        galleryView.setVisibility(GONE);
        textHint.setVisibility(VISIBLE);
        textHint.setText(R.string.gallery_no_photo);
    }

    private void loadFailed() {
        textHint.setVisibility(VISIBLE);
        textHint.setText(R.string.gallery_load_failed);
    }

    /**
     * Something should be done when clicking an image<br>
     * 点击图片后要做的事情
     */
    private void performClick(int position) {
        Log.d(TAG, "Detected click. position: " + position);
        galleryView.setVisibility(GONE);
        HashMap<String, Uri> map = mPhotoUriList.get(position);
        Uri uri;
        if ((uri = map.get(MIME_IMAGE)) != null) {
            // 图片处理 Photo processing
            Glide.with(requireContext())
                    .load(uri)
                    .priority(Priority.LOW)
                    .into(fullscreenView);
            fullscreenView.setVisibility(VISIBLE);
        } else if ((uri = map.get(MIME_VIDEO)) != null) {
            // 视频处理 Video processing
            // 播放视频
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(requireContext(), uri);
                mediaPlayer.prepareAsync();

            } catch (IOException e) {
                Log.e(TAG, "IOException thrown during preparing mediaPlayer");
                loadFailed();
                return;
            }
            mediaPlayer.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.start();
                bar.setMax(mediaPlayer.getDuration());
                progressBarUpdate();
                // 播放完成后修改按键提示
                mediaPlayer.setOnCompletionListener(mp -> setFooterBar(SubActivity.C_PLAY));
                // 显示视频组件
                videoView.setVisibility(VISIBLE);
                videoLayout.setVisibility(VISIBLE);
                // 修改按键提示为“暂停”
                setFooterBar(SubActivity.C_PAUSE);
            });

        }
    }

    private void closeVideoWindow() {
        if (mediaPlayer.isPlaying()) mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;
        timer.cancel();
        bar.setProgress(0);
        videoView.setVisibility(GONE);
        videoLayout.setVisibility(GONE);
        galleryView.setVisibility(VISIBLE);
    }

    /**
     * Regularly update progress bar
     */
    private void progressBarUpdate() {
        TextView tv = rootView.findViewById(R.id.videoPresentTime);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    bar.setProgress(currentPosition);
                    tv.setText(currentPosToString(currentPosition));
                }
            }
        }, 0, 1000);
    }

    private void moveFocus(int newPosition) {
        galleryView.scrollToPosition(newPosition);
        adapter.setSelectedPosition(newPosition);
    }

    private String currentPosToString(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (galleryView.getVisibility() == VISIBLE) {
                    int pos = adapter.getSelectedPosition();
                    int spanCount = ((GridLayoutManager) Objects.requireNonNull(galleryView.getLayoutManager())).getSpanCount();
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_UP -> {
                            if (pos >= spanCount) moveFocus(pos - spanCount);
                        }
                        case KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (pos < adapter.getItemCount() - spanCount) moveFocus(pos + spanCount);
                        }
                        case KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (pos > 0) moveFocus(pos - 1);
                        }
                        case KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (pos < adapter.getItemCount() - 1) moveFocus(pos + 1);
                        }
                    }
                    return true;
                } else if (videoLayout.getVisibility() == VISIBLE) {
                    int currentPos = mediaPlayer.getCurrentPosition();
                    int maxPos = mediaPlayer.getDuration();
                    int changeAmount = maxPos / 10;
                    int newPos = 0;

                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_LEFT -> {
                            // go backward 后退
                            if ((newPos = currentPos - changeAmount) < 0) newPos = 0;
                        }
                        case KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // go forward 前进
                            if ((newPos = currentPos + changeAmount) > maxPos) newPos = maxPos;
                        }
                    }
                    bar.setProgress(newPos);
                    mediaPlayer.seekTo(newPos);
                    return true;
                }
            }

            case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (galleryView.getVisibility() == VISIBLE) {
                    performClick(adapter.getSelectedPosition());
                } else {
                    if (videoLayout.getVisibility() == VISIBLE) {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            setFooterBar(SubActivity.C_RESUME);
                        } else {
                            mediaPlayer.start();
                            setFooterBar(SubActivity.C_PAUSE);
                        }
                    }
                }
            }
            case KeyEvent.KEYCODE_BACK -> {
                if (fullscreenView.getVisibility() == VISIBLE) {
                    fullscreenView.setVisibility(GONE);
                    galleryView.setVisibility(VISIBLE);
                    return true;
                } else if (videoLayout.getVisibility() == VISIBLE) {
                    closeVideoWindow();
                    setFooterBar();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Read all photos in the selected directory and use Glide Engine to load photos<br>
     * 在被授权的目录下读取所有照片，并使用Glide引擎加载照片
     * @param uri 文件夹的uri（由SAF提供）
     */
    private void readAllPhotos(Uri uri) {
        DocumentFile dir = DocumentFile.fromTreeUri(requireContext(), uri);
        new Thread(() -> {
            if (dir != null && dir.exists()) {
                DocumentFile[] files = dir.listFiles();
                mPhotoUriList = new ArrayList<>();
                for (DocumentFile file : files) {
                    Uri fileUri = file.getUri();
                    String fileName = file.getName();
                    String mimeType = file.getType();
                    String key;
                    Log.d(TAG, "Found file! Name:" + fileName + ", Type: " + mimeType);
                    // 判断是否为视频或图片，都不是就赋值key为null
                    if (mimeType == null) key = null;
                    else key = mimeType.startsWith(MIME_IMAGE)?
                            MIME_IMAGE : (mimeType.startsWith(MIME_VIDEO)?
                            MIME_VIDEO : null);
                    if (key != null) {
                        // Log.d(TAG, "Found a photo! Name:" + fileName + ", URI: " + fileUri);
                        HashMap<String, Uri> map = new HashMap<>();
                        map.put(key, fileUri);
                        mPhotoUriList.add(map);
                    }
                }
                try {
                    requireActivity().runOnUiThread(() -> {
                        // 如果Uri列表是空的，说明一张照片也没找到
                        if (mPhotoUriList.isEmpty()) {
                            noPhoto();
                        } else {
                            adapter = new GalleryAdapter(requireContext(), mPhotoUriList);
                            galleryView.setAdapter(adapter);
                            galleryView.setVisibility(VISIBLE);

                            textHint.setVisibility(GONE);
                            setFooterBar(SubActivity.L_OPTION);
                        }
                    });
                } catch (IllegalStateException e) {
                    // 如果用户在加载图片时点返回了，不要抛出错误
                    Log.w(TAG, "User closed gallery during loading");
                }
            } else {
                requireActivity().runOnUiThread(this::noPhoto);
            }
        }).start();
    }

    /**
     * Adapter for Gallery view<br>
     * 为相册界面自定义的适配器
     */
    private static class GalleryAdapter extends BaseAdapter {

        private final Context context;
        private final Drawable overlay;
        private final List<HashMap<String, Uri>> uris;

        public GalleryAdapter(@NonNull Context context, List<HashMap<String, Uri>> uris) {
            this.context = context;
            this.uris = uris;
            this.overlay = ContextCompat.getDrawable(context, R.drawable.ic_video);
        }

        private static class ViewHolder extends BaseAdapter.ViewHolder {
            ImageView iv;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.image);
            }
        }

        private void setImage(ImageView iv, int position) {
            iv.getOverlay().clear();
            // 取出Uri列表的数据
            HashMap<String, Uri> map = uris.get(position);
            Uri uri;
            boolean isVideo = (uri = map.get(MIME_VIDEO)) != null;
            if (isVideo || (uri = map.get(MIME_IMAGE)) != null)  {
                // 图片处理
                Glide.with(context)
                        .load(uri)
                        .frame(0)
                        .priority(Priority.LOW)
                        .into(new CustomTarget<Drawable>() {
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                iv.setImageDrawable(resource);
                                if (isVideo){
                                    addVideoIcon(iv);
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                iv.setImageDrawable(placeholder);
                            }
                        });
            }
        }

        private void addVideoIcon(ImageView imageView) {
            if (overlay != null) {
                imageView.post(() -> {
                    // 图标放到右下角
                    int badgeSize = imageView.getWidth() / 6;
                    int margin = badgeSize / 4;
                    overlay.setBounds(
                            imageView.getWidth() - badgeSize - margin,
                            imageView.getHeight() - badgeSize - margin,
                            imageView.getWidth() - margin,
                            imageView.getHeight() - margin
                    );
                    imageView.getOverlay().add(overlay);
                });
            } else Log.e(TAG, "Overlay is null!!!");
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.gallery_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return uris.size();
        }

        @Override
        public void onBindViewHolder(@NonNull BaseAdapter.ViewHolder holder, int position) {
            if (holder instanceof ViewHolder) {
                setImage(((ViewHolder) holder).iv, position);
            }
            super.onBindViewHolder(holder, position);
        }
    }

}