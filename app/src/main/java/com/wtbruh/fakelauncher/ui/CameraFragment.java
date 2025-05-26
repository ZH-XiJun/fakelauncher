package com.wtbruh.fakelauncher.ui;

import android.Manifest;
import android.os.Bundle;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.util.concurrent.ListenableFuture;
import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.MyFragment;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraFragment extends MyFragment {
    private final static String TAG = CameraFragment.class.getSimpleName();
    private CameraSelector nowCamera = CameraSelector.DEFAULT_BACK_CAMERA;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        return  new CameraFragment();
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (nowCamera == CameraSelector.DEFAULT_BACK_CAMERA) nowCamera = CameraSelector.DEFAULT_FRONT_CAMERA;
                else if (nowCamera == CameraSelector.DEFAULT_FRONT_CAMERA) nowCamera = CameraSelector.DEFAULT_BACK_CAMERA;
                startCamera();
                break;
        }
        return false;
    }

    private void init(){
        if (PrivilegeProvider.CheckPermission(getContext(), Manifest.permission.CAMERA)) startCamera();
        else unableToStartCamera();
    }

    /**
     * Start camera | 启动相机
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                PreviewView cameraView = rootView.findViewById(R.id.CameraPreview);
                preview.setSurfaceProvider(cameraView.getSurfaceProvider());
                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        getActivity(), // replace YourActivity with your actual activity name
                        nowCamera,
                        preview);
            } catch (Exception e) {
                Log.e(TAG, "An error occurred when opening camera: " + e);
                unableToStartCamera();
            }

        }, ContextCompat.getMainExecutor(getContext()));
    }

    /**
     * Show text to user when camera is unable to start.<br>
     * 如相机启动失败，向用户显示文字
     */
    private void unableToStartCamera() {
        rootView.findViewById(R.id.errorMessage).setVisibility(PreviewView.VISIBLE);
        rootView.findViewById(R.id.CameraPreview).setVisibility(PreviewView.INVISIBLE);
    }
}