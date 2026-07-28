package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;

public class ContactDetailFragment extends BaseFragment {
    private static final String ARG_NAME = "name";
    private static final String ARG_NUMBER = "number";

    public static ContactDetailFragment newInstance(String name, String number) {
        ContactDetailFragment fragment = new ContactDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_NUMBER, number);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        rootView = inflater.inflate(R.layout.fragment_contacts_detail, container, false);
        Bundle args = getArguments();
        ((TextView) rootView.findViewById(R.id.contact_name)).setText(
                args == null ? "" : args.getString(ARG_NAME, ""));
        ((TextView) rootView.findViewById(R.id.contact_number)).setText(
                args == null ? "" : args.getString(ARG_NUMBER, ""));
        setFooterBar(L_EMPTY, R_DEFAULT);
        return rootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) { return false; }
}
