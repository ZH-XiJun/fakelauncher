package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;
import com.wtbruh.fakelauncher.utils.CallHelper;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;

import java.util.ArrayList;
import java.util.List;

/** 系统通讯录列表：支持详情、拨号和删除。 */
public class ContactsFragment extends BaseFragment {
    private static final String TAG = ContactsFragment.class.getSimpleName();
    private RecyclerView contactsView;
    private SingleTextviewAdapter adapter;
    private final List<ContactEntry> contacts = new ArrayList<>();
    private final List<String> displayData = new ArrayList<>();

    public static ContactsFragment newInstance() { return new ContactsFragment(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        init();
        return rootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // This device maps the upper-left soft key to MENU. Keep it available
        // even when the list is empty, and use it to open the add-contact page.
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            requireSubActivity().fragmentStarter(AddContactFragment.newInstance());
            return true;
        }
        if (adapter == null || adapter.getItemCount() == 0) return false;
        int position = adapter.getSelectedPosition();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    contactsView.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                }
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < adapter.getItemCount() - 1) {
                    contactsView.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                }
                return true;
            }
            case KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                showOptionMenu();
                return true;
            }
            case KeyEvent.KEYCODE_CALL -> {
                callContact(contacts.get(position));
                return true;
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        contactsView = rootView.findViewById(R.id.contacts);
        if (!PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                || !loadContacts()) {
            noContact();
            return;
        }
        rootView.findViewById(R.id.contacts_textHint).setVisibility(View.INVISIBLE);
        contactsView.setVisibility(View.VISIBLE);
        contactsView.setLayoutManager(new LinearLayoutManager(requireContext()));
        contactsView.setFocusable(false);
        contactsView.setItemAnimator(null);
        adapter = new SingleTextviewAdapter(displayData);
        contactsView.setAdapter(adapter);
        setFooterBar(L_ADD, R_DEFAULT);
    }

    private void showOptionMenu() {
        int selected = adapter.getSelectedPosition();
        if (selected < 0 || selected >= contacts.size()) return;
        ContactEntry contact = contacts.get(selected);
        final int CALL = 0, DETAIL = 1, DELETE = 2;
        String[] selections = {getString(R.string.option_call), getString(R.string.option_detail),
                getString(R.string.option_delete)};
        requireSubActivity().showOptionMenu(selections, (keyCode, event, position, tv) -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                requireSubActivity().closeOptionMenu();
                switch (position) {
                    case CALL -> callContact(contact);
                    case DETAIL -> requireSubActivity().fragmentStarter(
                            ContactDetailFragment.newInstance(contact.name, contact.number));
                    case DELETE -> deleteContact(contact);
                }
            }
            return true;
        });
    }

    private void callContact(ContactEntry contact) {
        if (contact.number == null || contact.number.isEmpty()) return;
        CallHelper.placeCall(requireContext(), contact.number);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void deleteContact(ContactEntry contact) {
        if (!PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.WRITE_CONTACTS)) return;
        try {
            Uri lookupUri = ContactsContract.Contacts.getLookupUri(contact.contactId, contact.lookupKey);
            int deleted = lookupUri == null ? 0
                    : requireContext().getContentResolver().delete(lookupUri, null, null);
            if (deleted > 0) {
                int position = contacts.indexOf(contact);
                contacts.remove(position);
                displayData.remove(position);
                adapter.notifyDataSetChanged();
                if (contacts.isEmpty()) noContact();
                else adapter.setSelectedPosition(Math.min(position, contacts.size() - 1));
            }
        } catch (Exception e) { Log.e(TAG, "Unable to delete contact", e); }
    }

    private void noContact() {
        rootView.findViewById(R.id.contacts_textHint).setVisibility(View.VISIBLE);
        contactsView.setVisibility(View.INVISIBLE);
        setFooterBar(L_ADD, R_DEFAULT);
    }

    private boolean loadContacts() {
        contacts.clear();
        displayData.clear();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (Cursor cursor = requireContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC")) {
            if (cursor == null) return false;
            int id = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int lookup = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY);
            int name = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int number = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                String n = cursor.getString(name), p = cursor.getString(number);
                if (p == null || p.trim().isEmpty()) continue;
                if (n == null || n.trim().isEmpty()) n = p;
                ContactEntry entry = new ContactEntry(cursor.getLong(id), cursor.getString(lookup), n, p);
                contacts.add(entry);
                displayData.add(n + "  " + p);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to read contacts", e);
            return false;
        }
        return !contacts.isEmpty();
    }

    private static class ContactEntry {
        final long contactId;
        final String lookupKey, name, number;
        ContactEntry(long id, String lookupKey, String name, String number) {
            this.contactId = id; this.lookupKey = lookupKey; this.name = name; this.number = number;
        }
    }
}
