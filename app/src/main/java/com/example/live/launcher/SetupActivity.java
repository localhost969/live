package com.example.live.launcher;

import android.app.WallpaperManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.live.R;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.Map;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SetupActivity extends AppCompatActivity {

    private AppsAdapter adapter;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Uri uri = result.getData().getData();
                    if (uri == null) return;

                    final int flags = result.getData().getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try {
                        getContentResolver().takePersistableUriPermission(uri, flags);
                    } catch (Throwable ignored) {}

                    ImageWallpaperPrefs.setImageUri(SetupActivity.this, uri.toString());
                    renderImageState();
                }
            }
    );

    private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    // Refresh dashboard data if any permission was granted.
                    DashboardDataRefresher.refreshInBackground(SetupActivity.this);
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        final TextInputEditText pwd = findViewById(R.id.setup_password);
        final TextInputEditText pwd2 = findViewById(R.id.setup_password_confirm);
        final TextView error = findViewById(R.id.setup_error);

        RecyclerView rv = findViewById(R.id.setup_pinned_list);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppsAdapter(null, true);
        rv.setAdapter(adapter);

        List<AppInfo> apps = AppRepository.getLaunchableApps(this);
        adapter.submit(apps);
        adapter.setCheckedPackages(PinnedAppsStore.getPinned(this));

        findViewById(R.id.setup_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                error.setText("");

                String p1 = pwd.getText() == null ? "" : pwd.getText().toString();
                String p2 = pwd2.getText() == null ? "" : pwd2.getText().toString();

                if (!PasswordManager.isPasswordSet(SetupActivity.this)) {
                    if (!PasswordManager.meetsPolicy(p1)) {
                        error.setText(getString(R.string.password_policy));
                        return;
                    }
                    if (!p1.equals(p2)) {
                        error.setText(getString(R.string.password_mismatch));
                        return;
                    }
                    try {
                        PasswordManager.setPassword(SetupActivity.this, p1);
                    } catch (Exception e) {
                        error.setText(getString(R.string.password_set_failed));
                        return;
                    }
                }

                Set<String> pinned = adapter.getCheckedPackages();
                if (pinned.isEmpty()) {
                    error.setText(getString(R.string.pinned_empty));
                    return;
                }
                PinnedAppsStore.setPinned(SetupActivity.this, pinned);

                startActivity(new Intent(SetupActivity.this, LauncherActivity.class));
                finish();
            }
        });

        findViewById(R.id.pick_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, (Uri) null);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImageLauncher.launch(intent);
            }
        });

        findViewById(R.id.apply_lock_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySelectedImageToLockScreen();
            }
        });

        findViewById(R.id.set_dashboard_wallpaper).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // Ask permissions so dashboard can show calendar/temp. If denied, it will still show time/date.
            permissionsLauncher.launch(new String[] {
                Manifest.permission.READ_CALENDAR,
            });

            DashboardDataRefresher.refreshInBackground(SetupActivity.this);

            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new android.content.ComponentName(SetupActivity.this, DashboardWallpaperService.class));
            startActivity(intent);
            }
        });

        findViewById(R.id.setup_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PasswordManager.isPasswordSet(SetupActivity.this)) {
                    startActivity(new Intent(SetupActivity.this, LauncherActivity.class));
                }
                finish();
            }
        });

        renderPasswordSection();
        renderImageState();

        // Keep dashboard data reasonably fresh.
        DashboardDataRefresher.refreshInBackground(this);
    }

    private void renderPasswordSection() {
        View section = findViewById(R.id.password_section);
        TextView hint = findViewById(R.id.password_section_hint);

        boolean set = PasswordManager.isPasswordSet(this);
        section.setVisibility(set ? View.GONE : View.VISIBLE);
        hint.setText(set ? R.string.password_already_set : R.string.password_create_hint);
    }

    private void renderImageState() {
        TextView state = findViewById(R.id.image_state);
        String uri = ImageWallpaperPrefs.getImageUri(this);
        state.setText(uri == null ? R.string.image_not_set : R.string.image_set);
    }

    private void applySelectedImageToLockScreen() {
        String uriStr = ImageWallpaperPrefs.getImageUri(this);
        if (uriStr == null) {
            Toast.makeText(this, R.string.lock_apply_no_image, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(uriStr);
        WallpaperManager wm = WallpaperManager.getInstance(this);

        // On older Android versions, apps can't reliably set lock-screen wallpaper directly.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(this, R.string.lock_apply_requires_system_ui, Toast.LENGTH_LONG).show();
            openSystemWallpaperPicker(uri);
            return;
        }

        // Some devices / profiles block programmatic wallpaper changes.
        try {
            if (!wm.isSetWallpaperAllowed()) {
                Toast.makeText(this, R.string.lock_apply_not_allowed, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Throwable ignored) {
            // Keep going; OEMs can behave inconsistently.
        }

        // If a live wallpaper is active, it may show on the lock screen too (OEM-dependent).
        try {
            if (wm.getWallpaperInfo() != null) {
                Toast.makeText(this, R.string.lock_apply_live_wallpaper_note, Toast.LENGTH_LONG).show();
            }
        } catch (Throwable ignored) {}

        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(this, R.string.lock_apply_open_failed, Toast.LENGTH_LONG).show();
                return;
            }

            wm.setStream(is, null, true, WallpaperManager.FLAG_LOCK);
            Toast.makeText(this, R.string.lock_apply_success, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, R.string.lock_apply_failed_fallback, Toast.LENGTH_LONG).show();
            openSystemWallpaperPicker(uri);
        }
    }

    private void openSystemWallpaperPicker(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            intent.setDataAndType(uri, "image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.lock_apply_picker_title)));
        } catch (Throwable ignored) {
        }
    }

    private void loadImageInto(final ImageView target, final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(uri);
                    if (is != null) {
                        bmp = BitmapFactory.decodeStream(is);
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (is != null) {
                        try { is.close(); } catch (Throwable ignored) {}
                    }
                }

                final Bitmap finalBmp = bmp;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalBmp != null) target.setImageBitmap(finalBmp);
                        else target.setImageDrawable(null);
                    }
                });
            }
        }).start();
    }
}
