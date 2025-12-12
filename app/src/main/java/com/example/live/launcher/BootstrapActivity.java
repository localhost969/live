package com.example.live.launcher;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Android Studio run configurations expect a MAIN+LAUNCHER entry activity.
 * This activity immediately forwards into the actual HOME launcher.
 */
public final class BootstrapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, LauncherActivity.class));
        finish();
    }
}
