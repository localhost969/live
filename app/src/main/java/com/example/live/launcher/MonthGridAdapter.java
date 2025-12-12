package com.example.live.launcher;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Calendar;
import java.util.HashSet;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.live.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MonthGridAdapter extends RecyclerView.Adapter<MonthGridAdapter.DayVH> {

    public interface Listener {
        void onDayClicked(@NonNull DayCell day);
    }

    public static final class DayCell {
        public final long dayStartUtcMs;
        public final int dayKey; // yyyymmdd in device-local calendar
        public final int dayOfMonth;
        public final boolean inCurrentMonth;
        public final int dayOfWeek; // Calendar.SUNDAY, MONDAY, etc.
        public final boolean isHoliday;

        DayCell(long dayStartUtcMs, int dayKey, int dayOfMonth, boolean inCurrentMonth, int dayOfWeek, boolean isHoliday) {
            this.dayStartUtcMs = dayStartUtcMs;
            this.dayKey = dayKey;
            this.dayOfMonth = dayOfMonth;
            this.inCurrentMonth = inCurrentMonth;
            this.dayOfWeek = dayOfWeek;
            this.isHoliday = isHoliday;
        }
    }

    private final Listener listener;
    private final List<DayCell> cells = new ArrayList<>();

    public MonthGridAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<DayCell> newCells) {
        cells.clear();
        cells.addAll(newCells);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayVH holder, int position) {
        final DayCell cell = cells.get(position);
        holder.text.setText(cell.dayOfMonth <= 0 ? "" : String.valueOf(cell.dayOfMonth));
        // Is this the device's current day?
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        long todayStart = now.getTimeInMillis();
        boolean isToday = cell.dayStartUtcMs == todayStart;

        // Alpha for days not in month, but keep today's alpha full so highlight is visible
        holder.text.setAlpha(isToday ? 1.0f : (cell.inCurrentMonth ? 1.0f : 0.35f));

        boolean isSunday = cell.dayOfWeek == Calendar.SUNDAY;
        boolean shouldHighlightRed = cell.inCurrentMonth && (cell.isHoliday || isSunday);

        if (isToday) {
            holder.dayBg.setBackgroundResource(R.drawable.day_holiday_circle_bg);
            holder.text.setTextColor(ContextCompat.getColor(holder.text.getContext(), R.color.black));
            holder.text.setTypeface(null, Typeface.NORMAL);
        } else if (shouldHighlightRed) {
            if (cell.isHoliday) {
                holder.dayBg.setBackgroundResource(R.drawable.day_today_bg);
                holder.text.setTextColor(ContextCompat.getColor(holder.text.getContext(), R.color.white));
                holder.text.setTypeface(null, Typeface.BOLD);
            } else {
                holder.dayBg.setBackgroundResource(R.drawable.day_today_bg);
                holder.text.setTextColor(ContextCompat.getColor(holder.text.getContext(), R.color.white));
                holder.text.setTypeface(null, Typeface.NORMAL);
            }
        } else {
            holder.dayBg.setBackgroundResource(android.R.color.transparent);
            holder.text.setTextColor(ContextCompat.getColor(holder.text.getContext(), R.color.black));
            holder.text.setTypeface(null, Typeface.NORMAL);
        }

        // Keep background alpha in sync with text alpha for out-of-month dimming
        if (holder.dayBg != null) holder.dayBg.setAlpha(holder.text.getAlpha());

        if (holder.holidayDot != null) {
            holder.holidayDot.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null && cell.dayOfMonth > 0) listener.onDayClicked(cell);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static final class DayVH extends RecyclerView.ViewHolder {
        final TextView text;
        final View holidayDot;
        final View dayBg;

        DayVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.day_cell);
            holidayDot = itemView.findViewById(R.id.holiday_dot);
            dayBg = itemView.findViewById(R.id.day_bg);
        }
    }
}
