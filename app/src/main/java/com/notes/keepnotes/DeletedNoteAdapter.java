package com.notes.keepnotes;

import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DeletedNoteAdapter extends RecyclerView.Adapter<DeletedNoteAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    private List<DeletedNote> notes;
    private final Set<Long> selectedIds = new HashSet<>();
    private OnSelectionChangedListener listener;

    public DeletedNoteAdapter(List<DeletedNote> notes, OnSelectionChangedListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    public void updateNotes(List<DeletedNote> notes) {
        this.notes = notes;
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public void selectAll() {
        selectedIds.clear();
        for (DeletedNote n : notes) selectedIds.add(n.getId());
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedIds.size());
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(0);
    }

    public boolean isAllSelected() {
        return !notes.isEmpty() && selectedIds.size() == notes.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_deleted_note, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeletedNote note = notes.get(position);
        boolean selected = selectedIds.contains(note.getId());

        holder.title.setText(note.getTitle().isEmpty() ? "Untitled" : note.getTitle());
        holder.preview.setText(buildPlainPreview(note.getContent()));
        holder.checkbox.setChecked(selected);

        int daysLeft = daysRemaining(note.getDeletedDate());
        if (daysLeft <= 0) {
            holder.daysLeft.setText("Expires today");
        } else if (daysLeft == 1) {
            holder.daysLeft.setText("Expires in 1 day");
        } else {
            holder.daysLeft.setText("Expires in " + daysLeft + " days");
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedIds.contains(note.getId())) {
                selectedIds.remove(note.getId());
            } else {
                selectedIds.add(note.getId());
            }
            notifyItemChanged(position);
            if (listener != null) listener.onSelectionChanged(selectedIds.size());
        });
    }

    private String buildPlainPreview(String raw) {
        if (raw == null || raw.isEmpty()) return "No content";
        String CHECKLIST_SEP = "<!--CHECKLIST-->";
        String BELOW_SEP = "<!--BELOW-->";
        String display = raw;
        String checklistPreview = "";
        String belowPreview = "";
        if (display.contains(CHECKLIST_SEP)) {
            int sepIdx = display.indexOf(CHECKLIST_SEP);
            String checklistPart = display.substring(sepIdx + CHECKLIST_SEP.length()).trim();
            display = display.substring(0, sepIdx);
            if (checklistPart.contains(BELOW_SEP)) {
                int belowIdx = checklistPart.indexOf(BELOW_SEP);
                String belowPart = checklistPart.substring(belowIdx + BELOW_SEP.length()).trim();
                checklistPart = checklistPart.substring(0, belowIdx);
                belowPreview = belowPart.trim();
            }
            checklistPart = checklistPart.replaceAll("(0|1)\\|", "").trim();
            checklistPreview = checklistPart;
        }
        String plain;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            plain = Html.fromHtml(display, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        } else {
            plain = Html.fromHtml(display).toString().trim();
        }
        if (!checklistPreview.isEmpty()) {
            plain = plain.isEmpty() ? checklistPreview : plain + " " + checklistPreview;
        }
        if (!belowPreview.isEmpty()) {
            plain = plain.isEmpty() ? belowPreview : plain + " " + belowPreview;
        }
        if (plain.isEmpty()) return "No content";
        return plain.length() > 100 ? plain.substring(0, 100) + "…" : plain;
    }

    private int daysRemaining(String deletedDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date deleted = sdf.parse(deletedDate);
            if (deleted == null) return 30;
            long expiryMs = deleted.getTime() + 30L * 24 * 60 * 60 * 1000;
            long remaining = expiryMs - System.currentTimeMillis();
            return (int) (remaining / (24L * 60 * 60 * 1000));
        } catch (ParseException e) {
            return 30;
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        TextView title, preview, daysLeft;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkboxDeleted);
            title = itemView.findViewById(R.id.deletedNoteTitle);
            preview = itemView.findViewById(R.id.deletedNotePreview);
            daysLeft = itemView.findViewById(R.id.deletedNoteDaysLeft);
        }
    }
}
