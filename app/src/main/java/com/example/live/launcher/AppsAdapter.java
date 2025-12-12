package com.example.live.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.live.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.VH> {

    public interface Listener {
        void onAppClicked(AppInfo app);
        void onAppLongPressed(AppInfo app);
    }

    private final Listener listener;
    private final boolean showCheckbox;
    private final Set<String> checkedPackages = new HashSet<>();

    private final List<AppInfo> items = new ArrayList<>();

    public AppsAdapter(Listener listener, boolean showCheckbox) {
        this.listener = listener;
        this.showCheckbox = showCheckbox;
    }

    public void submit(List<AppInfo> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setCheckedPackages(Set<String> packages) {
        checkedPackages.clear();
        if (packages != null) checkedPackages.addAll(packages);
        notifyDataSetChanged();
    }

    public Set<String> getCheckedPackages() {
        return new HashSet<>(checkedPackages);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH h, int position) {
        final AppInfo app = items.get(position);
        h.label.setText(app.label);
        h.icon.setImageDrawable(app.icon);

        if (showCheckbox) {
            h.checkbox.setVisibility(View.VISIBLE);
            boolean checked = checkedPackages.contains(app.packageName);
            h.checkbox.setText(checked ? "✓" : "");
        } else {
            h.checkbox.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (showCheckbox) {
                    if (checkedPackages.contains(app.packageName)) checkedPackages.remove(app.packageName);
                    else checkedPackages.add(app.packageName);
                    notifyItemChanged(h.getBindingAdapterPosition());
                } else if (listener != null) {
                    listener.onAppClicked(app);
                }
            }
        });

        h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!showCheckbox && listener != null) {
                    listener.onAppLongPressed(app);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView checkbox;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
            checkbox = itemView.findViewById(R.id.app_check);
        }
    }
}
