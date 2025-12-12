package com.example.live.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.live.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public final class AllAppsActivity extends AppCompatActivity {

    private List<AppInfo> allApps;
    private AppsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_apps);

        allApps = AppRepository.getLaunchableApps(this);

        RecyclerView rv = findViewById(R.id.all_apps_list);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AppsAdapter(new AppsAdapter.Listener() {
            @Override
            public void onAppClicked(AppInfo app) {
                Intent launch = AppRepository.getLaunchIntent(AllAppsActivity.this, app.packageName);
                if (launch != null) startActivity(launch);
            }

            @Override
            public void onAppLongPressed(AppInfo app) {
                // no-op
            }
        }, false);
        rv.setAdapter(adapter);

        adapter.submit(allApps);

        final TextInputEditText search = findViewById(R.id.search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.close_all_apps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void filter(CharSequence query) {
        String q = query == null ? "" : query.toString().trim().toLowerCase();
        if (q.isEmpty()) {
            adapter.submit(allApps);
            return;
        }

        List<AppInfo> out = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (app.label.toLowerCase().contains(q)) out.add(app);
        }
        adapter.submit(out);
    }
}
