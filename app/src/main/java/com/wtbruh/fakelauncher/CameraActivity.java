package com.wtbruh.fakelauncher;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

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
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        CameraActivity.this, // replace YourActivity with your actual activity name
                        cameraSelector,
                        preview);
            } catch (Exception e) {
                Log.e(TAG, "An error occurred when opening camera: " + e);
                unableToStartCamera();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Show text to user when camera is unable to start.
     */
    private void unableToStartCamera() {
        findViewById(R.id.errorMessage).setVisibility(PreviewView.VISIBLE);
        findViewById(R.id.CameraPreview).setVisibility(PreviewView.INVISIBLE);

    }
}