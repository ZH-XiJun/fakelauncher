package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ContactsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContactsFragment extends BaseFragment{

    private final static String TAG = ContactsFragment.class.getSimpleName();

    RecyclerView contactsView;
    SingleTextviewAdapter adapter;
    List<String> data;

    public ContactsFragment() {
        // Required empty public constructor
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        init();
        return rootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int position = adapter.getSelectedPosition();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    contactsView.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < adapter.getItemCount() - 1) {
                    contactsView.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        contactsView = rootView.findViewById(R.id.contacts);
        if (!PrivilegeProvider.checkPermission(getContext(), Manifest.permission.READ_CONTACTS) || (data = getBasicContact()) == null) {
            noContact();
            return;
        }
        rootView.findViewById(R.id.contacts_textHint).setVisibility(View.INVISIBLE);
        contactsView.setVisibility(View.VISIBLE);

        contactsView.setLayoutManager(new LinearLayoutManager(requireContext()));
        contactsView.setFocusable(false);
        // contactsView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        contactsView.setItemAnimator(null);

        adapter = new SingleTextviewAdapter(data);
        contactsView.setAdapter(adapter);
    }

    private void noContact() {
        rootView.findViewById(R.id.contacts_textHint).setVisibility(View.VISIBLE);
        contactsView.setVisibility(View.INVISIBLE);
        setFooterBar(L_EMPTY);
    }

    @Nullable
    private List<String> getBasicContact() {
        Cursor cursor = requireContext().getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null    );
        if (cursor == null) return null;
        List<String> list = new ArrayList<>();

        if (cursor.moveToFirst()) {
            int displayNameColumn = cursor
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            do {
                String contactName = cursor.getString(displayNameColumn);
                list.add(contactName);
                Log.d(TAG, "getting name: "+contactName);

            } while (cursor.moveToNext());
        }
        Log.d(TAG, "list:"+list);
        cursor.close();
        return list;
    }
}