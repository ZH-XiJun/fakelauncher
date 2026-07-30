package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.utils.PrivilegeProvider;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.ArrayList;

/** Contact editor operated entirely with the hardware keypad. */
public class AddContactFragment extends BaseFragment {
    private TextView nameView;
    private TextView numberView;
    private boolean editingName = true;

    public static AddContactFragment newInstance() {
        return new AddContactFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        rootView = inflater.inflate(R.layout.fragment_contact_add, container, false);
        nameView = rootView.findViewById(R.id.contact_add_name);
        numberView = rootView.findViewById(R.id.contact_add_number);
        updateFocus();
        setFooterBar(L_SAVE, R_EDITTEXT);
        return rootView;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            TextView target = editingName ? nameView : numberView;
            target.append(String.valueOf(keyCode - KeyEvent.KEYCODE_0));
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> {
                editingName = true;
                updateFocus();
                return true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN -> {
                editingName = false;
                updateFocus();
                return true;
            }
            case KeyEvent.KEYCODE_BACK -> {
                TextView target = editingName ? nameView : numberView;
                String value = target.getText().toString();
                if (value.isEmpty()) return false;
                target.setText(value.substring(0, value.length() - 1));
                return true;
            }
            case KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SOFT_LEFT,
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                saveContact();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void updateFocus() {
        nameView.setAlpha(editingName ? 1f : .55f);
        numberView.setAlpha(editingName ? .55f : 1f);
    }

    private void saveContact() {
        String name = nameView.getText().toString().trim();
        String number = numberView.getText().toString().trim();
        if (name.isEmpty() || number.isEmpty()) {
            UIHelper.showCustomDialog(requireContext(), R.string.contact_input_required, null);
            return;
        }
        if (!PrivilegeProvider.checkPermission(requireContext(), Manifest.permission.WRITE_CONTACTS)) {
            UIHelper.showCustomDialog(requireContext(), R.string.dialog_save_fail, null);
            return;
        }
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());
        operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());
        operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());
        try {
            requireContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
            UIHelper.showCustomDialog(requireContext(), R.string.dialog_save_success, null).setOnDismissListener(dialogInterface -> requireActivity().onBackPressed());

        } catch (RemoteException | OperationApplicationException | SecurityException e) {
            UIHelper.showCustomDialog(requireContext(), R.string.dialog_save_fail, null);
        }
    }
}