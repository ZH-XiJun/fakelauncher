package com.wtbruh.fakelauncher.ui;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.utils.MyFragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GalleryFragment extends MyFragment {

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
        // Only show "no photo" hint at present
        // todo: gallery show.
        rootView.findViewById(R.id.gridView).setVisibility(INVISIBLE);
        rootView.findViewById(R.id.noPhoto).setVisibility(VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }
}