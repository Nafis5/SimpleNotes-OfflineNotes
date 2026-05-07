package com.notes.keepnotes;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupManager {

    private static final String TAG = "BackupManager";
    private static final String BACKUP_FOLDER = "database_backups";
    private static final String BACKUP_FILENAME = "backup.json";
    static final String PREF_NAME = "backup_prefs";
    static final String KEY_LAST_BACKUP_DATE = "last_backup_date";
    static final String KEY_BACKUP_EMAIL = "backup_email";
    static final String KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled";

    private final Context context;
    private final NoteDatabase db;

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = new NoteDatabase(this.context);
    }

    public boolean isAutoBackupEnabled() {
        return getPrefs().getBoolean(KEY_AUTO_BACKUP_ENABLED, false);
    }

    public void setAutoBackupEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply();
    }

    public String getSavedEmail() {
        return getPrefs().getString(KEY_BACKUP_EMAIL, "");
    }

    public void saveEmail(String email) {
        getPrefs().edit().putString(KEY_BACKUP_EMAIL, email.trim().toLowerCase()).apply();
    }

    /**
     * Returns true if 7+ days have passed since the last backup.
     * Backup only runs when the app is opened (called from onResume), never from background.
     */
    public boolean shouldBackupNow() {
        String lastDateStr = getPrefs().getString(KEY_LAST_BACKUP_DATE, "");
        if (lastDateStr.isEmpty()) return true;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date lastDate = sdf.parse(lastDateStr);
            if (lastDate == null) return true;
            long diffMs = System.currentTimeMillis() - lastDate.getTime();
            long days = diffMs / (24L * 60 * 60 * 1000);
            return days >= 7;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing last backup date", e);
            return true;
        }
    }

    private void setLastBackupDate() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        getPrefs().edit().putString(KEY_LAST_BACKUP_DATE, today).apply();
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String sanitizeEmail(String email) {
        return email.replace("@", "_at_").replace(".", "_dot_");
    }

    private String notesToJson() throws JSONException {
        List<Note> notes = db.getAllNotes();
        JSONArray array = new JSONArray();
        for (Note note : notes) {
            JSONObject obj = new JSONObject();
            obj.put("title", note.getTitle() != null ? note.getTitle() : "");
            obj.put("content", note.getContent() != null ? note.getContent() : "");
            obj.put("date", note.getDate() != null ? note.getDate() : "");
            obj.put("time", note.getTime() != null ? note.getTime() : "");
            array.put(obj);
        }
        JSONObject root = new JSONObject();
        root.put("backup_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        root.put("notes", array);
        return root.toString(2);
    }

    public void performBackup(String email, BackupCallback callback) {
        String json;
        try {
            json = notesToJson();
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing notes", e);
            if (callback != null) callback.onFailure("Failed to serialize notes");
            return;
        }

        File tempFile = new File(context.getFilesDir(), BACKUP_FILENAME);
        try {
            FileWriter writer = new FileWriter(tempFile);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error writing temp backup file", e);
            if (callback != null) callback.onFailure("Failed to write backup file");
            return;
        }

        String path = BACKUP_FOLDER + "/" + sanitizeEmail(email) + "/" + BACKUP_FILENAME;
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);

        ref.delete().addOnCompleteListener(task -> uploadFile(tempFile, ref, callback));
    }

    private void uploadFile(File file, StorageReference ref, BackupCallback callback) {
        ref.putFile(Uri.fromFile(file))
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Backup uploaded successfully");
                    setLastBackupDate();
                    file.delete();
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed: " + e.getMessage());
                    file.delete();
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void restoreFromBackup(String email, boolean replace, RestoreCallback callback) {
        String path = BACKUP_FOLDER + "/" + sanitizeEmail(email) + "/" + BACKUP_FILENAME;
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);

        File localFile = new File(context.getFilesDir(), "restore_" + BACKUP_FILENAME);

        ref.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    try {
                        FileInputStream fis = new FileInputStream(localFile);
                        byte[] data = new byte[(int) localFile.length()];
                        fis.read(data);
                        fis.close();

                        String json = new String(data, StandardCharsets.UTF_8);
                        JSONObject root = new JSONObject(json);
                        JSONArray array = root.getJSONArray("notes");

                        if (replace) {
                            db.deleteAllNotes();
                        }

                        int count = 0;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Note note = new Note(
                                    obj.optString("title", ""),
                                    obj.optString("content", ""),
                                    obj.optString("date", ""),
                                    obj.optString("time", "")
                            );
                            db.addNote(note);
                            count++;
                        }

                        localFile.delete();
                        Log.d(TAG, "Restored " + count + " notes (replace=" + replace + ")");
                        if (callback != null) callback.onSuccess(count);

                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Error parsing backup", e);
                        localFile.delete();
                        if (callback != null) callback.onFailure("Failed to parse backup file");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Download failed: " + e.getMessage());
                    localFile.delete();
                    if (callback != null) callback.onFailure("No backup found for this email");
                });
    }

    public interface BackupCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface RestoreCallback {
        void onSuccess(int restoredCount);
        void onFailure(String error);
    }
}
