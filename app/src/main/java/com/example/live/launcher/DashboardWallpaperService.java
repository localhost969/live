package com.example.live.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DashboardWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new DashEngine();
    }

    private final class DashEngine extends Engine {

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                drawFrame();
                // Update every minute.
                handler.postDelayed(this, 60_000L);
            }
        };

        private boolean visible;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect bounds = new Rect();

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            paint.setColor(0xFF000000);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                handler.removeCallbacks(tick);
                handler.post(tick);
            } else {
                handler.removeCallbacks(tick);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(tick);
            visible = false;
        }

        private void drawFrame() {
            if (!visible) return;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c == null) return;

                c.getClipBounds(bounds);
                c.drawColor(0xFFFFFFFF);

                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                String date = new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(new Date());

                float x = dp(24);
                float y = dp(80);

                paint.setTextSize(sp(52));
                c.drawText(time, x, y, paint);

                paint.setTextSize(sp(18));
                c.drawText(date, x, y + dp(32), paint);

                String temp = DashboardDataStore.getTemperature(DashboardWallpaperService.this);
                String next = DashboardDataStore.getNextEvent(DashboardWallpaperService.this);

                float yy = y + dp(80);
                paint.setTextSize(sp(16));

                if (!TextUtils.isEmpty(temp)) {
                    c.drawText(temp, x, yy, paint);
                    yy += dp(26);
                }

                if (!TextUtils.isEmpty(next)) {
                    c.drawText(truncate(next, 32), x, yy, paint);
                }

            } catch (Throwable ignored) {
            } finally {
                if (c != null) {
                    try { holder.unlockCanvasAndPost(c); } catch (Throwable ignored) {}
                }
            }
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        private float sp(float v) {
            return v * getResources().getDisplayMetrics().scaledDensity;
        }

        private String truncate(String s, int max) {
            if (s == null) return "";
            if (s.length() <= max) return s;
            return s.substring(0, max - 1) + "…";
        }
    }
}
