package com.notes.keepnotes;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Set;

public class RecoverNotesActivity extends AppCompatActivity {

    private NoteDatabase db;
    private DeletedNoteAdapter adapter;
    private List<DeletedNote> deletedNotes;
    private RecyclerView recyclerView;
    private TextView emptyTrashText;
    private View recoverActionBar;
    private TextView btnSelectAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int nightMode = getSharedPreferences("theme_prefs", MODE_PRIVATE)
                .getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(nightMode);
        setContentView(R.layout.activity_recover_notes);

        db = new NoteDatabase(this);

        recyclerView = findViewById(R.id.deletedNotesRecyclerView);
        emptyTrashText = findViewById(R.id.emptyTrashText);
        recoverActionBar = findViewById(R.id.recoverActionBar);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        ImageButton btnBack = findViewById(R.id.btnRecoverBack);
        View btnRecover = findViewById(R.id.btnRecoverSelected);
        View btnDeleteForever = findViewById(R.id.btnDeleteForeverSelected);

        deletedNotes = db.getDeletedNotes();
        adapter = new DeletedNoteAdapter(deletedNotes, this::onSelectionChanged);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateEmptyState();

        btnBack.setOnClickListener(v -> finish());

        btnSelectAll.setOnClickListener(v -> {
            if (adapter.isAllSelected()) {
                adapter.clearSelection();
            } else {
                adapter.selectAll();
            }
        });

        btnRecover.setOnClickListener(v -> {
            Set<Long> ids = adapter.getSelectedIds();
            if (ids.isEmpty()) {
                Toast.makeText(this, "No notes selected", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Recover Notes")
                    .setMessage("Recover " + ids.size() + " selected note" + (ids.size() > 1 ? "s" : "") + "?")
                    .setPositiveButton("Recover", (dialog, which) -> {
                        for (long id : ids) db.restoreNote(id);
                        Toast.makeText(this,
                                ids.size() + " note" + (ids.size() > 1 ? "s" : "") + " recovered",
                                Toast.LENGTH_SHORT).show();
                        refreshList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnDeleteForever.setOnClickListener(v -> {
            Set<Long> ids = adapter.getSelectedIds();
            if (ids.isEmpty()) {
                Toast.makeText(this, "No notes selected", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Delete Forever")
                    .setMessage("Permanently delete " + ids.size() + " selected note" + (ids.size() > 1 ? "s" : "") + "? This cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        for (long id : ids) db.permanentlyDeleteNote(id);
                        Toast.makeText(this,
                                ids.size() + " note" + (ids.size() > 1 ? "s" : "") + " permanently deleted",
                                Toast.LENGTH_SHORT).show();
                        refreshList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void onSelectionChanged(int count) {
        recoverActionBar.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        btnSelectAll.setText(adapter.isAllSelected() ? "Deselect All" : "Select All");
    }

    private void refreshList() {
        deletedNotes = db.getDeletedNotes();
        adapter.updateNotes(deletedNotes);
        recoverActionBar.setVisibility(View.GONE);
        btnSelectAll.setText("Select All");
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (deletedNotes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyTrashText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyTrashText.setVisibility(View.GONE);
        }
    }
}
