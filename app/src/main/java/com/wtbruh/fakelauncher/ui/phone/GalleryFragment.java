package com.wtbruh.fakelauncher.ui.phone;

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

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.ui.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.utils.MyFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Gallery 相册
 */
public class GalleryFragment extends MyFragment {

    private final static String TAG = GalleryFragment.class.getSimpleName();
    public final static String MIME_IMAGE = "image/";
    public final static String MIME_VIDEO = "video/";
    private GridView gridView;
    private ImageView fullscreenView;
    private TextView textHint;
    private SurfaceView videoView;
    private MediaPlayer mediaPlayer = null;
    private int mNowSelectedViewPosition = 0;
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
        gridView = rootView.findViewById(R.id.gridView);
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
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
        gridView.setVisibility(GONE);
        textHint.setVisibility(VISIBLE);
        textHint.setText(R.string.gallery_no_photo);
    }

    private void loadFailed() {
        textHint.setVisibility(VISIBLE);
        textHint.setText(R.string.gallery_load_failed);
    }

    /**
     * Init of GridView items<br>
     * GridView 元素初始化
     */
    private void itemInit() {
        // Record position for opening file
        // 记录下当前被选中的位置以实现打开对应文件
        gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                mNowSelectedViewPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        // File opening
        // 打开文件
        gridView.setOnItemClickListener((adapterView, view, position, l) -> {
            Log.d(TAG, "Detected click. position: "+position);
            gridView.setVisibility(GONE);
            HashMap<String, Uri> map = mPhotoUriList.get(position);
            Uri uri;
            if ((uri = map.get(MIME_IMAGE)) != null) {
                // 图片处理 Photo processing
                Glide.with(getContext())
                        .load(uri)
                        .priority(Priority.LOW)
                        .into(fullscreenView);
                fullscreenView.setVisibility(VISIBLE);
            } else if ((uri = map.get(MIME_VIDEO)) != null) {
                mediaPlayer = new MediaPlayer();
                // 视频处理 Video processing
                try {
                    mediaPlayer.setDataSource(getContext(), uri);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    Log.e(TAG, "IOException thrown during preparing mediaPlayer");
                    loadFailed();
                    return;
                }
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mediaPlayer -> setFooterBar(SubActivity.C_PLAY));
                videoView.setVisibility(VISIBLE);
                setFooterBar(SubActivity.C_PAUSE);
            }
        });
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:

                View view = gridView.getChildAt(mNowSelectedViewPosition);
                if (view != null) {
                    view.performClick();

                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (videoView.getVisibility() == VISIBLE) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        setFooterBar(SubActivity.C_RESUME);
                    }
                    else {
                        mediaPlayer.start();
                        setFooterBar(SubActivity.C_PAUSE);
                    }
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (fullscreenView.getVisibility() == VISIBLE) {
                    fullscreenView.setVisibility(GONE);
                    gridView.setVisibility(VISIBLE);
                    return true;
                } else if (videoView.getVisibility() == VISIBLE) {
                    if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    videoView.setVisibility(GONE);
                    gridView.setVisibility(VISIBLE);
                    setFooterBar();
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * Read all photos in the selected directory and use Glide Engine to load photos<br>
     * 在被授权的目录下读取所有照片，并使用Glide引擎加载照片
     * @param uri 文件夹的uri（由SAF提供）
     */
    private void readAllPhotos(Uri uri) {
        DocumentFile dir = DocumentFile.fromTreeUri(getContext(), uri);
        new Thread(() -> {
            if (dir != null && dir.exists()) {
                DocumentFile[] files = dir.listFiles();
                mPhotoUriList = new ArrayList<>();
                for (DocumentFile file : files) {
                    Uri fileUri = file.getUri();
                    String fileName = file.getName();
                    String mimeType = file.getType();

                    Log.d(TAG, "Found file! Name:" + fileName + ", Type: " + mimeType);
                    // 判断是否为视频或图片，都不是就赋值key为null
                    String key = mimeType.startsWith(MIME_IMAGE)?
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
                            gridView.setAdapter(new ImageAdapter(getContext(), mPhotoUriList));
                            textHint.setVisibility(GONE);
                            gridView.setVisibility(VISIBLE);
                            setFooterBar(SubActivity.L_OPTION);
                            itemInit();
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
    static class ImageAdapter extends ArrayAdapter<HashMap<String, Uri>> {
        private final Context context;
        private final Drawable overlay;
        private final List<HashMap<String, Uri>> uris;
        public ImageAdapter(@NonNull Context context, List<HashMap<String, Uri>> uris) {
            super(context, R.layout.gridview_item, uris);
            this.context = context;
            this.uris = uris;
            this.overlay = ContextCompat.getDrawable(context, R.drawable.vector_gridview_video);
        }
        // AI说这样能提高运行效率
        private static class ViewHolder {
            ImageView imageView;
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
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.gridview_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = convertView.findViewById(R.id.image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                holder.imageView.getOverlay().clear();
            }

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
                                holder.imageView.setImageDrawable(resource);
                                if (isVideo){
                                    addVideoIcon(holder.imageView);
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                holder.imageView.setImageDrawable(placeholder);
                            }
                        });
            }
            return convertView;
        }
    }
}