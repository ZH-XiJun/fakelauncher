package com.wtbruh.fakelauncher;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Objects;

public class ActivityShortcut extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Objects.equals(getIntent().getAction(), Intent.ACTION_CREATE_SHORTCUT)) {
            Intent intent = new Intent(this, SplashActivity.class).setAction(Intent.ACTION_MAIN);

            setResult(RESULT_OK,
                    ShortcutManagerCompat.createShortcutResultIntent(this,
                            new ShortcutInfoCompat.Builder(this, "fakeUI")
                                    .setIntent(intent)
                                    .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher_round))
                                    .setShortLabel(getString(R.string.shortcut_name))
                                    .build()
                    ));
        }
        finish();
    }

}
