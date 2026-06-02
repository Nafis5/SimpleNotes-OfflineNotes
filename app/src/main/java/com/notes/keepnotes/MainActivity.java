package com.notes.keepnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private NoteDatabase db;
    private List<Note> notes;
    private TextView emptyStateText;
    private LinearLayout searchBar;
    private EditText searchEditText;
    private LinearLayout selectionBar;
    private TextView selectionCount;
    private BackupManager backupManager;
    private SwitchCompat switchAutoBackup;
    private AppLockManager appLockManager;
    private SwitchCompat switchAppLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyThemeFromPrefs();
        setContentView(R.layout.activity_main);
        db = new NoteDatabase(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        recyclerView = findViewById(R.id.notesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        NavigationView navView = findViewById(R.id.nav_view);
        ImageButton btnHamburger = findViewById(R.id.btnHamburger);
        ImageButton btnSearch = findViewById(R.id.btnSearch);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notes = new ArrayList<>();
        noteAdapter = new NoteAdapter(notes, this);
        recyclerView.setAdapter(noteAdapter);

        // FAB - launch editor for new note
        fab.setOnClickListener(v -> {
            if (!CheckPremiumStatus.isPremium && notes.size() >= 10) {
                showUpgradeDialogAddNotes();
                return;
            }
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            startActivity(intent);
        });

        // Hamburger opens drawer
        btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(navView));

        // Selection bar
        selectionBar = findViewById(R.id.selectionBar);
        selectionCount = findViewById(R.id.selectionCount);
        ImageButton btnCloseSelection = findViewById(R.id.btnCloseSelection);
        ImageButton btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        btnCloseSelection.setOnClickListener(v -> exitSelectionMode());

        btnDeleteSelected.setOnClickListener(v -> {
            Set<Long> ids = noteAdapter.getSelectedIds();
            if (ids.isEmpty()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Delete Notes")
                    .setMessage("Delete " + ids.size() + " selected note" + (ids.size() > 1 ? "s" : "") + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        for (long id : ids) {
                            db.deleteNote(id);
                        }
                        exitSelectionMode();
                        loadNotes();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        searchBar = findViewById(R.id.searchBar);
        searchEditText = findViewById(R.id.searchEditText);
        ImageButton btnCloseSearch = findViewById(R.id.btnCloseSearch);

        // Bottom bar - search toggle
        btnSearch.setOnClickListener(v -> {
            if (searchBar.getVisibility() == View.VISIBLE) {
                closeSearch();
            } else {
                searchBar.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(searchEditText, 0);
            }
        });

        // Close search
        btnCloseSearch.setOnClickListener(v -> closeSearch());

        // Real-time filtering
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                noteAdapter.filter(s.toString());
                // Show/hide empty state based on filtered results
                if (noteAdapter.getItemCount() == 0) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    emptyStateText.setText("No notes found");
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        backupManager = new BackupManager(this);
        appLockManager = new AppLockManager(this);

        // Wire up the app lock switch
        switchAppLock = (SwitchCompat) navView.getMenu()
                .findItem(R.id.nav_app_lock).getActionView();
        boolean[] suppressLock = {false};
        if (switchAppLock != null) {
            switchAppLock.setChecked(appLockManager.isAppLockEnabled());
            switchAppLock.setOnCheckedChangeListener((btn, isChecked) -> {
                if (suppressLock[0]) return;
                if (!CheckPremiumStatus.isPremium) {
                    suppressLock[0] = true;
                    switchAppLock.setChecked(false);
                    suppressLock[0] = false;
                    showUpgradeDialog();
                    return;
                }
                if (isChecked) {
                    if (!appLockManager.isDeviceSecure()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Device Lock Required")
                                .setMessage("App lock uses your phone's screen lock (PIN, pattern, password, or fingerprint). Please set up a device lock in your phone's Settings first, then enable this.")
                                .setPositiveButton("OK", (d, w) -> {})
                                .setOnDismissListener(d -> {
                                    suppressLock[0] = true;
                                    switchAppLock.setChecked(false);
                                    suppressLock[0] = false;
                                })
                                .show();
                    } else {
                        appLockManager.setAppLockEnabled(true);
                        Toast.makeText(this, "App lock enabled", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    appLockManager.setAppLockEnabled(false);
                    Toast.makeText(this, "App lock disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Wire up the auto backup switch
        switchAutoBackup = (SwitchCompat) navView.getMenu()
                .findItem(R.id.nav_auto_backup).getActionView();
        // suppressListener flag prevents recursive calls when setting switch programmatically
        boolean[] suppressListener = {false};
        if (switchAutoBackup != null) {
            switchAutoBackup.setChecked(backupManager.isAutoBackupEnabled());
            switchAutoBackup.setOnCheckedChangeListener((btn, isChecked) -> {
                if (suppressListener[0]) return;
                if (!CheckPremiumStatus.isPremium) {
                    suppressListener[0] = true;
                    switchAutoBackup.setChecked(false);
                    suppressListener[0] = false;
                    showUpgradeDialog();
                    return;
                }
                if (isChecked) {
                    showEmailDialog(switchAutoBackup, suppressListener);
                } else {
                    backupManager.setAutoBackupEnabled(false);
                    Toast.makeText(this, "Automatic backup disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Drawer menu items
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_app_lock) {
                if (!CheckPremiumStatus.isPremium) { showUpgradeDialog(); return true; }
                if (switchAppLock != null) switchAppLock.setChecked(!switchAppLock.isChecked());
                return true;
            } else if (id == R.id.nav_auto_backup) {
                if (!CheckPremiumStatus.isPremium) { showUpgradeDialog(); return true; }
                if (switchAutoBackup != null) {
                    switchAutoBackup.setChecked(!switchAutoBackup.isChecked());
                }
                return true;
            } else if (id == R.id.nav_restore) {
                if (!CheckPremiumStatus.isPremium) { drawerLayout.closeDrawers(); showUpgradeDialog(); return true; }
                drawerLayout.closeDrawers();
                showRestoreDialog();
                return true;
            }
            if (id == R.id.nav_themes) {
                if (!CheckPremiumStatus.isPremium) { drawerLayout.closeDrawers(); showUpgradeDialog(); return true; }
                drawerLayout.closeDrawers();
                showThemeDialog();
                return true;
            } else if (id == R.id.nav_recover_deleted) {
                if (!CheckPremiumStatus.isPremium) { drawerLayout.closeDrawers(); showUpgradeDialog(); return true; }
                drawerLayout.closeDrawers();
                startActivity(new Intent(this, RecoverNotesActivity.class));
                return true;
            }
            drawerLayout.closeDrawers();
            Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
            return true;
        });

        loadNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (searchBar != null && searchBar.getVisibility() == View.VISIBLE) {
            closeSearch();
        }
        loadNotes();
        triggerAutoBackupIfNeeded();
    }

    private void showUpgradeDialogAddNotes() {
        new AlertDialog.Builder(this)
                .setTitle("Upgrade to Premium")
                .setMessage("To add more notes please upgrade to premium")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    startActivity(new Intent(this, SubscriptionActivity2.class));
                })
                .show();
    }

    private void showUpgradeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Upgrade to Premium")
                .setMessage("To use this feature please upgrade to premium")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    startActivity(new Intent(this, SubscriptionActivity2.class));
                })
                .show();
    }

    private void applyThemeFromPrefs() {
        int mode = getSharedPreferences("theme_prefs", MODE_PRIVATE)
                .getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void showThemeDialog() {
        int currentMode = getSharedPreferences("theme_prefs", MODE_PRIVATE)
                .getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int checkedItem = currentMode == AppCompatDelegate.MODE_NIGHT_YES ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(
                        new String[]{"Light Theme", "Dark Theme"},
                        checkedItem,
                        null
                )
                .setPositiveButton("Apply", (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    int mode = selected == 1
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO;
                    getSharedPreferences("theme_prefs", MODE_PRIVATE)
                            .edit().putInt("night_mode", mode).apply();
                    AppCompatDelegate.setDefaultNightMode(mode);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void triggerAutoBackupIfNeeded() {
        if (!backupManager.isAutoBackupEnabled()) return;
        if (!backupManager.shouldBackupNow()) return;
        String email = backupManager.getSavedEmail();
        if (email.isEmpty()) return;
        backupManager.performBackup(email, new BackupManager.BackupCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Notes backed up successfully", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Backup failed: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showEmailDialog(SwitchCompat sw, boolean[] suppressListener) {
        EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email address");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        String savedEmail = backupManager.getSavedEmail();
        if (!savedEmail.isEmpty()) emailInput.setText(savedEmail);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        emailInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Automatic Backup")
                .setMessage("Enter the email address to associate with your backup.")
                .setView(emailInput)
                .setPositiveButton("Enable", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                        suppressListener[0] = true;
                        sw.setChecked(false);
                        suppressListener[0] = false;
                        return;
                    }
                    backupManager.saveEmail(email);
                    backupManager.setAutoBackupEnabled(true);
                    Toast.makeText(this, "Automatic backup enabled", Toast.LENGTH_SHORT).show();
                    // Do an immediate backup on first enable
                    backupManager.performBackup(email, new BackupManager.BackupCallback() {
                        @Override public void onSuccess() {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                    "Initial backup complete", Toast.LENGTH_SHORT).show());
                        }
                        @Override public void onFailure(String error) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                    "Backup failed: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    suppressListener[0] = true;
                    sw.setChecked(false);
                    suppressListener[0] = false;
                })
                .setOnCancelListener(dialog -> {
                    suppressListener[0] = true;
                    sw.setChecked(false);
                    suppressListener[0] = false;
                })
                .show();
    }

    private void showRestoreDialog() {
        String savedEmail = backupManager.getSavedEmail();
        EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email address");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (!savedEmail.isEmpty()) emailInput.setText(savedEmail);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        emailInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Restore Notes")
                .setMessage("Enter the email address associated with your backup.")
                .setView(emailInput)
                .setPositiveButton("Next", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showRestoreModeDialog(email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRestoreModeDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("How would you like to restore?")
                .setMessage(null)
                .setPositiveButton("Merge Notes", (dialog, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Merge Notes?")
                            .setMessage("All notes from the backup will be added to your existing notes. This may result in duplicates.")
                            .setPositiveButton("Merge", (d, w) -> performRestore(email, false))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Replace Existing Notes", (dialog, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Replace All Notes?")
                            .setMessage("All current notes on this device will be permanently deleted and replaced with the backup. Any notes not backed up will be lost.")
                            .setPositiveButton("Replace", (d, w) -> performRestore(email, true))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void performRestore(String email, boolean replace) {
        Toast.makeText(this, "Restoring...", Toast.LENGTH_SHORT).show();
        backupManager.restoreFromBackup(email, replace, new BackupManager.RestoreCallback() {
            @Override public void onSuccess(int restoredCount) {
                runOnUiThread(() -> {
                    loadNotes();
                    Toast.makeText(MainActivity.this,
                            restoredCount + " notes restored successfully", Toast.LENGTH_LONG).show();
                });
            }
            @Override public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadNotes() {
        notes = db.getAllNotes();
        noteAdapter.updateNotes(notes);

        if (notes.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.getId());
        startActivity(intent);
    }

    @Override
    public void onNoteLongClick(Note note, int position) {
        // Now handled by selection mode in the adapter
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count > 0) {
            selectionBar.setVisibility(View.VISIBLE);
            selectionCount.setText(count + " selected");
        } else {
            exitSelectionMode();
        }
    }

    private void exitSelectionMode() {
        noteAdapter.clearSelection();
        selectionBar.setVisibility(View.GONE);
    }

    private void closeSearch() {
        searchEditText.setText("");
        searchBar.setVisibility(View.GONE);
        noteAdapter.filter("");
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        // Restore empty state text
        if (notes.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText("Tap + to create a note");
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (noteAdapter.isInSelectionMode()) {
            exitSelectionMode();
        } else if (searchBar.getVisibility() == View.VISIBLE) {
            closeSearch();
        } else if (drawerLayout.isDrawerOpen(findViewById(R.id.nav_view))) {
            drawerLayout.closeDrawers();
        } else {
            finishAffinity();
        }
    }
}
