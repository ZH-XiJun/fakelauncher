package com.wtbruh.fakelauncher.ui.fragment.phone;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileDescriptorOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.common.util.concurrent.ListenableFuture;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.fragment.settings.SubSettingsFragment;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraFragment extends BaseFragment {
    private final static String TAG = CameraFragment.class.getSimpleName();
    private CameraSelector nowCamera = CameraSelector.DEFAULT_BACK_CAMERA;
    private File tempFile;
    // Views
    private PreviewView cameraView;
    private ImageView capturePreview;

    private ImageCapture imageCapture;
    private VideoCapture<?> videoCapture;
    private Recorder recorder;
    private Recording recording;
    private ProcessCameraProvider cameraProvider;

    // true为拍照，false为录像
    private boolean mode = true;
    private boolean isCapturing = false;
    private long captureTime = 0;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        init();
        return rootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> {
                if (nowCamera == CameraSelector.DEFAULT_BACK_CAMERA)
                    nowCamera = CameraSelector.DEFAULT_FRONT_CAMERA;
                else if (nowCamera == CameraSelector.DEFAULT_FRONT_CAMERA)
                    nowCamera = CameraSelector.DEFAULT_BACK_CAMERA;
                startCamera();
            }
            case KeyEvent.KEYCODE_MENU -> {
                if (! isCapturing) {
                    if (cameraView.getVisibility() == VISIBLE) {
                        showOptionMenu();
                    } else if (capturePreview.getVisibility() == VISIBLE) {
                        saveCapturedFile();
                    }
                }
            }
            case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> capture();
            case KeyEvent.KEYCODE_BACK -> {
                if (capturePreview.getVisibility() == VISIBLE) {
                    capturePreview.setVisibility(GONE);
                    cameraView.setVisibility(VISIBLE);
                    setDefaultFooterBar();
                    return true;
                } else if (isCapturing) {
                    return true;
                }
            }
        }
        return false;
    }

    private void init(){
        if (PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.CAMERA)) {
            startCamera();
            capturePreview = rootView.findViewById(R.id.capturePreview);
        }
        else unableToStartCamera();
    }

    /**
     * Start camera | 启动相机
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                cameraView = rootView.findViewById(R.id.CameraPreview);
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());

                // Use cases init
                QualitySelector qs = QualitySelector.fromOrderedList(
                        List.of(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                );
                recorder = new Recorder.Builder()
                        .setExecutor(ContextCompat.getMainExecutor(requireContext()))
                        .setQualitySelector(qs)
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                imageCapture = new ImageCapture.Builder().setTargetRotation(cameraView.getDisplay().getRotation()).build();

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        requireActivity(),
                        nowCamera,
                        preview);
            } catch (Exception e) {
                Log.e(TAG, "An error occurred when opening camera: " + e);
                unableToStartCamera();
            }

        }, ContextCompat.getMainExecutor(requireContext()));
        setDefaultFooterBar();
    }

    private void bindToLifecycle() {
        if (cameraProvider.isBound(mode? videoCapture : imageCapture)) cameraProvider.unbind(mode? videoCapture : imageCapture);
        cameraProvider.bindToLifecycle(
                requireActivity(),
                nowCamera,
                mode? imageCapture : videoCapture);
    }

    /**
     * Set default footer bar<br>
     * 恢复默认底部栏
     */
    private void setDefaultFooterBar() {
        setFooterBar(L_OPTION, mode? C_SHOOT : C_RECORD, R_DEFAULT);
    }
    /**
     * Show text to user when camera is unable to start.<br>
     * 如相机启动失败，向用户显示文字
     */
    private void unableToStartCamera() {
        rootView.findViewById(R.id.camera_textHint).setVisibility(VISIBLE);
        rootView.findViewById(R.id.CameraPreview).setVisibility(GONE);
    }

    /**
     * 显示选项菜单
     */
    private void showOptionMenu(){
        int[] selections = {R.string.camera_option_record};
        if (!mode) selections[0] = R.string.camera_option_shoot;

        ((SubActivity) requireActivity()).showOptionMenu(
                selections,
                (keyCode, event, position, tv) -> {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (tv != null) {
                                if (requireContext().getString(R.string.camera_option_record).contentEquals(tv.getText())) {
                                    tv.setText(R.string.camera_option_shoot);
                                    setFooterBar(C_RECORD);
                                    mode = false;
                                } else {
                                    tv.setText(R.string.camera_option_record);
                                    setFooterBar(C_SHOOT);
                                    mode = true;
                                }
                                bindToLifecycle();
                            }
                        }
                    }
                    return true;
                }
        );
    }

    /**
     * 拍照/录像
     */
    private void capture() {
        if (cameraView == null || cameraView.getVisibility() != VISIBLE) return;
        if (!isCapturing) {
            isCapturing = true;
            setFooterBar(L_EMPTY);
            if (!cameraProvider.isBound(mode? imageCapture : videoCapture)) bindToLifecycle();
            if (mode) {
                tempFile = new File(requireContext().getCacheDir(), "temp.jpg");
                // Reverse when using front camera
                // 当用前置摄像头拍照时翻转照片
                ImageCapture.Metadata metadata = new ImageCapture.Metadata();
                metadata.setReversedHorizontal(nowCamera == CameraSelector.DEFAULT_FRONT_CAMERA);
                // 照片输出参数
                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(tempFile)
                        .setMetadata(metadata)
                        .build();
                // 拍照
                imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        isCapturing = false;
                        captureTime = System.currentTimeMillis();
                        ImageView iv = rootView.findViewById(R.id.capturePreview);
                        cameraView.setVisibility(GONE);
                        iv.setVisibility(VISIBLE);
                        setFooterBar(L_SAVE);
                        // Disable cache, or Glide will not load the newest file
                        // 禁用缓存，否则Glide会加载缓存的旧图片
                        RequestOptions options = new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true);
                        Glide.with(requireContext())
                                .load(tempFile)
                                .apply(options)
                                .into(iv);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error on capturing: " + exception);
                        UIHelper.showDialog(requireContext(), R.string.dialog_capture_fail, null);
                        if (isCapturing) isCapturing = false;
                        setDefaultFooterBar();
                    }
                });
            } else {
                saveCapturedFile();
            }
        } else if (!mode) recording.stop();
    }

    @SuppressLint("NewApi")
    private void saveCapturedFile() {
        InputStream i = null;
        OutputStream o = null;
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(captureTime);
        String filename;
        try {
            // 从SharedPreferences拿到用户给我们授权访问的目录的Uri，作为照片存储目录
            // Get gallery URI granted by user for the save destination
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String uriStr = sp.getString(SubSettingsFragment.PREF_GALLERY_ACCESS_URI, "");
            if (uriStr.isEmpty()) {
                // 如果拿到个空的，说明用户根本没授权啊，要么就是用户趁我不注意偷偷改了SharedPreference
                // If URI is empty, that meas user didn't grant access, fuck you user!
                throw new SecurityException("User didn't grant access to any folder for saving files");
            }
            Uri uri = Uri.parse(uriStr);
            DocumentFile folder = DocumentFile.fromTreeUri(requireContext(), uri);
            if (folder == null) {
                throw new IOException("Folder that user granted access to not found. Uri: "+uriStr);
            }

            if (mode) {
                // 拍照
                filename = "IMG_" + time + ".jpg";
                DocumentFile destFile = folder.createFile("image/jpeg", filename);
                if (destFile == null) throw new IOException("Failed to create file for captured photo");

                i = new FileInputStream(tempFile);
                o = requireContext().getContentResolver().openOutputStream(destFile.getUri());
                if (o == null) throw new NullPointerException("Got null OutputStream");

                byte[] buffer = new byte[2048];
                int byteRead;
                while ((byteRead = i.read(buffer)) != -1) o.write(buffer, 0, byteRead);

                i.close();
                o.flush();
                o.close();
                showSaveResultDialog(true);
            } else {
                // 录像
                filename = "VID_" + time + ".mp4";
                DocumentFile destFile = folder.createFile("video/mp4", filename);
                if (destFile == null) throw new IOException("Failed to create file for recorded video");
                ParcelFileDescriptor pfd = requireContext().getContentResolver().openFileDescriptor(destFile.getUri(), "w");
                if (pfd == null) throw new IOException("Failed to get file descriptor for created file. File Uri: "+destFile.getUri());

                FileDescriptorOutputOptions options = new FileDescriptorOutputOptions.Builder(pfd).build();

                PendingRecording pr = recorder.prepareRecording(requireContext(), options);
                if (PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
                    pr.withAudioEnabled();
                }
                recording = pr.start(
                        ContextCompat.getMainExecutor(requireContext()),
                        videoRecordEvent -> {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                setFooterBar(C_STOP, R_EMPTY);
                            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                isCapturing = false;
                                setFooterBar(R_DEFAULT);
                                showSaveResultDialog(!((VideoRecordEvent.Finalize) videoRecordEvent).hasError());
                                if (recording != null) {
                                    recording.close();
                                    recording = null;
                                }
                            }
                        }
                );

            }
        } catch (Exception e) {
            isCapturing = false;
            showSaveResultDialog(false);
            Log.e(TAG, "Error on saving photo/video: "+e);
        } finally {
            try {
                if (i != null) i.close();
                if (o != null) o.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on closing IO stream: " + e);
            }
        }
    }

    private void showSaveResultDialog(boolean result) {
        int message = result? R.string.dialog_save_success : mode? R.string.dialog_save_fail : R.string.dialog_record_fail;
        Dialog dialog = UIHelper.showDialog(requireContext(), message, null);
        dialog.setOnDismissListener(dialogInterface -> {
            capturePreview.setVisibility(GONE);
            cameraView.setVisibility(VISIBLE);
            setDefaultFooterBar();
        });
    }

}