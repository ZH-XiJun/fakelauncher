package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;

import java.util.List;

public class OptionMenuFragment extends BaseFragment {
    private final onKeyUpListener listener;
    private final List<String> selections;
    private SingleTextviewAdapter adapter;
    private RecyclerView menuView;

    public OptionMenuFragment(List<String> selections, onKeyUpListener listener) {
        this.listener = listener;
        this.selections = selections;
    }

    public interface onKeyUpListener {
        boolean onKeyUp(int keyCode, KeyEvent event, int position, TextView tv);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        int position = getCurrentPosition();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    menuView.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < adapter.getItemCount() - 1) {
                    menuView.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_BACK -> ((SubActivity) requireActivity()).closeOptionMenu();
        }
        return listener.onKeyUp(keyCode, event, position, adapter.getTextView());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_option, container, false);
        init();
        return rootView;
    }

    private void init() {
        adapter = new SingleTextviewAdapter(selections);
        menuView = rootView.findViewById(R.id.menuView);
        menuView.setAdapter(adapter);
        menuView.setItemAnimator(null);
        menuView.setLayoutManager(new LinearLayoutManager(requireContext()));
        menuView.setFocusable(false);
        adapter.smallerTextViewHeight(2);
    }

    private int getCurrentPosition(){
        if (adapter != null) return adapter.getSelectedPosition();
        else return 0;
    }

}
