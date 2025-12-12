package com.example.live.launcher;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.live.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuickContactsAdapter extends RecyclerView.Adapter<QuickContactsAdapter.ViewHolder> {

    private final Context context;
    private final List<QuickContact> contacts;
    private final Listener listener;
    private final ItemTouchHelper touchHelper;
    private String selectedContactId = null;
    private boolean inEditMode = false;

    public interface Listener {
        void onContactsChanged(List<QuickContact> contacts);
    }

    public QuickContactsAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.contacts = new ArrayList<>();
        
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                       @NonNull RecyclerView.ViewHolder viewHolder) {
                int drag = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                          ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(drag, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                 @NonNull RecyclerView.ViewHolder viewHolder,
                                 @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(contacts, from, to);
                notifyItemMoved(from, to);
                if (listener != null) listener.onContactsChanged(new ArrayList<>(contacts));
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Swipe not supported; use long-press menu instead
            }
        };
        
        this.touchHelper = new ItemTouchHelper(callback);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        touchHelper.attachToRecyclerView(recyclerView);
    }

    public void submitList(List<QuickContact> newContacts) {
        contacts.clear();
        contacts.addAll(newContacts);
        notifyDataSetChanged();
    }

    public void exitEditMode() {
        if (inEditMode) {
            inEditMode = false;
            selectedContactId = null;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_quick_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickContact contact = contacts.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final View actions;
        private final ImageView dragHandle;
        private final ImageView menuButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.contact_name);
            actions = itemView.findViewById(R.id.contact_actions);
            dragHandle = itemView.findViewById(R.id.drag_handle);
            menuButton = itemView.findViewById(R.id.contact_menu);
        }

        public void bind(QuickContact contact) {
            nameText.setText("Call " + contact.name + " ?");

            // Check if in edit mode
            boolean isSelected = inEditMode && contact.id.equals(selectedContactId);
            if (actions != null) actions.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Tap: call when not editing; otherwise exit edit mode.
            itemView.setOnClickListener(v -> {
                if (inEditMode) {
                    exitEditMode();
                } else {
                    callContact(contact);
                }
            });

            // Long-press: show actions for this contact.
            itemView.setOnLongClickListener(v -> {
                inEditMode = true;
                selectedContactId = contact.id;
                notifyDataSetChanged();
                return true;
            });

            // Drag handle
            dragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (inEditMode && contact.id.equals(selectedContactId)) {
                        touchHelper.startDrag(this);
                    }
                }
                return false;
            });

            // Menu for delete
            menuButton.setOnClickListener(v -> showMenu(contact));
        }

        private void callContact(QuickContact contact) {
            if (!hasPermission(Manifest.permission.CALL_PHONE)) {
                return;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_CALL,
                        Uri.parse("tel:" + Uri.encode(contact.number)));
                context.startActivity(intent);
            } catch (Throwable t) {
                // Ignore
            }
        }

        private void showMenu(QuickContact contact) {
            PopupMenu menu = new PopupMenu(context, menuButton);
            menu.inflate(R.menu.menu_quick_contact);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete) {
                    removeContact(contact);
                    return true;
                }
                return false;
            });
            menu.show();
        }

        private void removeContact(QuickContact contact) {
            int pos = contacts.indexOf(contact);
            if (pos >= 0) {
                contacts.remove(pos);
                notifyItemRemoved(pos);
                QuickContactsStore.removeContact(context, contact.id);
                inEditMode = false;
                selectedContactId = null;
                if (listener != null) listener.onContactsChanged(new ArrayList<>(contacts));
            }
        }
    }

    private boolean hasPermission(String perm) {
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED;
    }
}

