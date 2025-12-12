package com.example.live.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.live.R;

/** ViewPager2 adapter with exactly 3 pages: Calendar (index 0), Focus Home (index 1), Others (index 2). */
public final class LauncherPagerAdapter extends RecyclerView.Adapter<LauncherPagerAdapter.PageVH> {

    public interface PageBoundListener {
        void onCalendarPageBound(@NonNull View calendarRoot);

        void onHomePageBound(@NonNull View homeRoot);

        void onOthersPageBound(@NonNull View othersRoot);
    }

    static final int PAGE_CALENDAR = 0;
    static final int PAGE_HOME = 1;
    static final int PAGE_OTHERS = 2;

    private final PageBoundListener listener;

    public LauncherPagerAdapter(@NonNull PageBoundListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == PAGE_CALENDAR) {
            View v = inflater.inflate(R.layout.page_calendar, parent, false);
            return new PageVH(v);
        } else if (viewType == PAGE_HOME) {
            View v = inflater.inflate(R.layout.page_focus_home, parent, false);
            return new PageVH(v);
        }
        View v = inflater.inflate(R.layout.page_others, parent, false);
        return new PageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH holder, int position) {
        if (listener == null) return;
        if (position == PAGE_CALENDAR) {
            listener.onCalendarPageBound(holder.itemView);
        } else if (position == PAGE_HOME) {
            listener.onHomePageBound(holder.itemView);
        } else {
            listener.onOthersPageBound(holder.itemView);
        }
    }

    static final class PageVH extends RecyclerView.ViewHolder {
        PageVH(@NonNull View itemView) {
            super(itemView);
        }
    }
}
