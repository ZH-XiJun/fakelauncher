package com.wtbruh.fakelauncher.ui.fragment.phone;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wtbruh.fakelauncher.R;
import com.wtbruh.fakelauncher.SubActivity;
import com.wtbruh.fakelauncher.ui.VolumeActivity;
import com.wtbruh.fakelauncher.ui.fragment.BaseFragment;
import com.wtbruh.fakelauncher.ui.view.SingleTextviewAdapter;
import com.wtbruh.fakelauncher.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 伪装设置二级菜单（主菜单"设置"输入0000进入）：
 * 情景模式 / 音量调节 / 亮度调节 / 完整设置。
 * 功能机风格：黑底白字列表，方向键 + 确定键操作，禁触。
 */
public class FakeSettingsFragment extends BaseFragment {

    private RecyclerView menuView;
    private SingleTextviewAdapter adapter;
    private final List<String> itemNames = new ArrayList<>();
    private final List<String> itemTargets = new ArrayList<>();

    public FakeSettingsFragment() { }

    public static FakeSettingsFragment newInstance() {
        return new FakeSettingsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_fake_settings, container, false);
        init();
        return rootView;
    }

    private void init() {
        itemNames.clear();
        itemTargets.clear();
        itemNames.add(getString(R.string.fake_settings_scene));   // 情景模式
        itemTargets.add("scene");
        itemNames.add(getString(R.string.fake_settings_volume));  // 音量调节
        itemTargets.add("volume");
        itemNames.add(getString(R.string.fake_settings_brightness)); // 亮度调节
        itemTargets.add("brightness");
        itemNames.add(getString(R.string.fake_settings_screen_timeout)); // 熄屏设置
        itemTargets.add("screen_timeout");

        menuView = rootView.findViewById(R.id.fake_settings_list);
        menuView.setLayoutManager(new LinearLayoutManager(requireContext()));
        menuView.setFocusable(false);
        menuView.setItemAnimator(null);
        // 功能机风格：禁触
        menuView.setOnTouchListener((v, e) -> true);
        adapter = new SingleTextviewAdapter(itemNames);
        menuView.setAdapter(adapter);
        setFooterBar(L_DEFAULT, R_DEFAULT);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (adapter == null || adapter.getItemCount() == 0) {
            return keyCode == KeyEvent.KEYCODE_BACK;
        }
        int position = adapter.getSelectedPosition();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (position > 0) {
                    menuView.scrollToPosition(position - 1);
                    adapter.setSelectedPosition(position - 1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (position < adapter.getItemCount() - 1) {
                    menuView.scrollToPosition(position + 1);
                    adapter.setSelectedPosition(position + 1);
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                openItem(position);
                return true;
            case KeyEvent.KEYCODE_BACK:
                // 清空回退栈（密码等），直接回到主菜单
                requireSubActivity().clearBackStackToMenu();
                return true;
            default:
                return false;
        }
    }

    private void openItem(int position) {
        if (position < 0 || position >= itemTargets.size()) return;
        String target = itemTargets.get(position);
        SubActivity activity = requireSubActivity();
        switch (target) {
            case "scene":
                activity.fragmentStarter(SceneModeFragment.newInstance());
                break;
            case "volume":
                UIHelper.startIntent(requireActivity(), VolumeActivity.class);
                break;
            case "brightness":
                activity.fragmentStarter(BrightnessFragment.newInstance());
                break;
            case "screen_timeout":
                activity.fragmentStarter(ScreenTimeoutFragment.newInstance());
                break;

            default:
                break;
        }
    }
}
