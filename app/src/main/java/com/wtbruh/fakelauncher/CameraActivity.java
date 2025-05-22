package com.wtbruh.fakelauncher;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.activity.EdgeToEdge;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.wtbruh.fakelauncher.utils.MyAppCompatActivity;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

public class CameraActivity extends MyAppCompatActivity {
    private CameraSelector nowCamera = CameraSelector.DEFAULT_BACK_CAMERA;
    private final static String TAG = CameraActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);
        if (PrivilegeProvider.CheckPermission(this, Manifest.permission.CAMERA)) startCamera();
        else unableToStartCamera();
    }

    /**
     * Start camera | 启动相机
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                PreviewView cameraView = findViewById(R.id.CameraPreview);
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());
                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        CameraActivity.this, // replace YourActivity with your actual activity name
                        nowCamera,
                        preview);
            } catch (Exception e) {
                Log.e(TAG, "An error occurred when opening camera: " + e);
                unableToStartCamera();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Show text to user when camera is unable to start.<br>
     * 如相机启动失败，向用户显示文字
     */
    private void unableToStartCamera() {
        findViewById(R.id.errorMessage).setVisibility(PreviewView.VISIBLE);
        findViewById(R.id.CameraPreview).setVisibility(PreviewView.INVISIBLE);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (nowCamera == CameraSelector.DEFAULT_BACK_CAMERA) nowCamera = CameraSelector.DEFAULT_FRONT_CAMERA;
                else if (nowCamera == CameraSelector.DEFAULT_FRONT_CAMERA) nowCamera = CameraSelector.DEFAULT_BACK_CAMERA;
                startCamera();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

}