package com.wtbruh.fakelauncher.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.utils.MyFragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessageFragment extends MyFragment {

    public static final String URI_HEADER = "content://sms/";
    public static final String PATH_INBOX = "inbox";
    public static final String PATH_SEND = "sent";
    public static final String PATH_DRAFT = "draft";
    public static final String PATH_OUTBOX = "outbox";
    public static final String PATH_FAILED = "failed";
    public static final String PATH_QUEUED = "queued";

    public MessageFragment() {
        // Required empty public constructor
    }

    public static MessageFragment newInstance() {
        return new MessageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_message, container, false);
        init();
        return rootView;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    private void init() {
        // todo:Message
        setFooterBar(SubActivity.L_EMPTY);
        readSMS();
    }

    private void readSMS() {
    }
}