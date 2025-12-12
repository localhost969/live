package com.example.live.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AppRepository {

    private AppRepository() {}

    public static List<AppInfo> getLaunchableApps(Context context) {
        PackageManager pm = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        List<AppInfo> apps = new ArrayList<>(resolveInfos.size());

        for (ResolveInfo ri : resolveInfos) {
            String packageName = ri.activityInfo.packageName;
            CharSequence labelCs = ri.loadLabel(pm);
            String label = labelCs == null ? packageName : labelCs.toString();
            apps.add(new AppInfo(packageName, label, ri.loadIcon(pm)));
        }

        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        return apps;
    }

    public static Intent getLaunchIntent(Context context, String packageName) {
        return context.getPackageManager().getLaunchIntentForPackage(packageName);
    }
}
