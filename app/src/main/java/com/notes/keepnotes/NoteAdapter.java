package com.notes.keepnotes;

import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> notes;
    private List<Note> allNotes;
    private OnNoteClickListener listener;
    private Set<Long> selectedIds = new HashSet<>();
    private boolean isSelectionMode = false;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteLongClick(Note note, int position);
        void onSelectionChanged(int count);
    }

    public NoteAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.allNotes = new ArrayList<>(notes);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);

        String title = note.getTitle();
        if (title == null || title.isEmpty()) {
            title = "Untitled";
        }
        holder.titleTextView.setText(title);

        String content = note.getContent();
        if (content != null && !content.isEmpty()) {
            // Strip checklist and below-checklist data before preview
            String displayContent = content;
            String checklistSep = "<!--CHECKLIST-->";
            String belowSep = "<!--BELOW-->";
            String checklistPreview = "";
            String belowPreview = "";
            if (displayContent.contains(checklistSep)) {
                int sepIndex = displayContent.indexOf(checklistSep);
                String checklistPart = displayContent.substring(sepIndex + checklistSep.length()).trim();
                displayContent = displayContent.substring(0, sepIndex);
                // Extract below-checklist content
                if (checklistPart.contains(belowSep)) {
                    int belowIdx = checklistPart.indexOf(belowSep);
                    String belowPart = checklistPart.substring(belowIdx + belowSep.length()).trim();
                    checklistPart = checklistPart.substring(0, belowIdx).trim();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        belowPreview = Html.fromHtml(belowPart, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                    } else {
                        belowPreview = Html.fromHtml(belowPart).toString().trim();
                    }
                }
                // Build a short checklist preview
                String[] items = checklistPart.split("\n");
                StringBuilder clPreview = new StringBuilder();
                for (String item : items) {
                    if (item.length() >= 2 && item.indexOf('|') >= 0) {
                        boolean checked = item.charAt(0) == '1';
                        String text = item.substring(item.indexOf('|') + 1);
                        clPreview.append(checked ? "☑ " : "☐ ").append(text).append("  ");
                    }
                }
                checklistPreview = clPreview.toString().trim();
            }
            String plainText;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                plainText = Html.fromHtml(displayContent, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            } else {
                plainText = Html.fromHtml(displayContent).toString().trim();
            }
            if (!checklistPreview.isEmpty()) {
                plainText = plainText.isEmpty() ? checklistPreview : plainText + " " + checklistPreview;
            }
            if (!belowPreview.isEmpty()) {
                plainText = plainText.isEmpty() ? belowPreview : plainText + " " + belowPreview;
            }
            if (!plainText.isEmpty()) {
                holder.contentPreviewTextView.setVisibility(View.VISIBLE);
                String preview = plainText.length() > 80 ? plainText.substring(0, 80) + "..." : plainText;
                holder.contentPreviewTextView.setText(preview);
            } else {
                holder.contentPreviewTextView.setVisibility(View.GONE);
            }
        } else {
            holder.contentPreviewTextView.setVisibility(View.GONE);
        }

        String dateTime = "";
        if (note.getDate() != null) {
            dateTime = note.getDate();
        }
        if (note.getTime() != null && !note.getTime().isEmpty()) {
            dateTime += ", " + note.getTime();
        }
        holder.dateTextView.setText(dateTime);

        // Selection highlight
        if (selectedIds.contains(note.getId())) {
            holder.itemView.setBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(R.color.selectionHighlight));
        } else {
            holder.itemView.setBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(R.color.backGroundColor));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (isSelectionMode) {
                    toggleSelection(note.getId());
                    notifyItemChanged(position);
                    listener.onSelectionChanged(selectedIds.size());
                } else {
                    listener.onNoteClick(note);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                if (!isSelectionMode) {
                    isSelectionMode = true;
                    selectedIds.clear();
                }
                toggleSelection(note.getId());
                notifyItemChanged(position);
                listener.onSelectionChanged(selectedIds.size());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notes == null ? 0 : notes.size();
    }

    public void updateNotes(List<Note> newNotes) {
        this.allNotes = new ArrayList<>(newNotes);
        this.notes = new ArrayList<>(newNotes);
        notifyDataSetChanged();
    }

    public void toggleSelection(long noteId) {
        if (selectedIds.contains(noteId)) {
            selectedIds.remove(noteId);
        } else {
            selectedIds.add(noteId);
        }
        if (selectedIds.isEmpty()) {
            isSelectionMode = false;
        }
    }

    public void clearSelection() {
        selectedIds.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public boolean isInSelectionMode() {
        return isSelectionMode;
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            notes = new ArrayList<>(allNotes);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            List<Note> filtered = new ArrayList<>();
            for (Note note : allNotes) {
                String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
                String content = note.getContent() != null ? note.getContent().toLowerCase() : "";
                // Strip HTML/checklist/below for search matching
                String checklistSep = "<!--checklist-->";
                String belowSep = "<!--below-->";
                if (content.contains(checklistSep)) {
                    int sepIdx = content.indexOf(checklistSep);
                    String checklistPart = content.substring(sepIdx + checklistSep.length());
                    content = content.substring(0, sepIdx);
                    // Extract below-checklist text for searching
                    if (checklistPart.contains(belowSep)) {
                        int belowIdx = checklistPart.indexOf(belowSep);
                        String belowPart = checklistPart.substring(belowIdx + belowSep.length());
                        checklistPart = checklistPart.substring(0, belowIdx);
                        content = content + " " + belowPart;
                    }
                    checklistPart = checklistPart.replaceAll("(0|1)\\|", "");
                    content = content + " " + checklistPart;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    content = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString().toLowerCase();
                } else {
                    content = Html.fromHtml(content).toString().toLowerCase();
                }
                if (title.contains(lowerQuery) || content.contains(lowerQuery)) {
                    filtered.add(note);
                }
            }
            notes = filtered;
        }
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentPreviewTextView;
        TextView dateTextView;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.noteTitle);
            contentPreviewTextView = itemView.findViewById(R.id.noteContentPreview);
            dateTextView = itemView.findViewById(R.id.noteDate);
        }
    }
}
