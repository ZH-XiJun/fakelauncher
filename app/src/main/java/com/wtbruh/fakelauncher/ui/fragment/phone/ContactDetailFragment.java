package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;

public class ContactDetailFragment extends BaseFragment {
    public static ContactDetailFragment newInstance() {
        return new ContactDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_contacts_detail, container, false);
        init();
        return rootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    private void init() {

    }
}
