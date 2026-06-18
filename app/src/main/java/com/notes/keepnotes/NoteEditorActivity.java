package com.notes.keepnotes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class NoteEditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "note_id";
    private static final long NEW_NOTE_ID = -1;

    private EditText editTitle;
    private EditText editContent;
    private EditText editContentBelow;
    private NoteDatabase db;
    private long noteId = NEW_NOTE_ID;
    private boolean isEditMode = false;
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private TextView btnBold;
    private TextView btnItalic;
    private TextView btnToolbarHeading;
    private int activeHeadingLevel = 0;
    private LinearLayout checklistContainer;
    private ImageButton btnToolbarCheckbox;
    private boolean isChecklistActive = false;
    private ImageButton btnToolbarBullet;
    private boolean isBulletActive = false;
    private static final int BULLET_GAP_WIDTH = 20;
    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 101;

    private static final String CHECKLIST_SEPARATOR = "\n<!--CHECKLIST-->\n";
    private static final String BELOW_SEPARATOR = "\n<!--BELOW-->\n";

    private static final String FONT_MARKER_PREFIX = "<!--FONT:";
    private static final String FONT_MARKER_SUFFIX = "-->";
    private String selectedFontKey = "default";

    // Font keys (resource names). "default" = system font, "inter" is free, the rest are premium.
    private static final String[] FONT_KEYS = {
            "default", "inter", "caveat", "lora", "merriweather",
            "nunito", "patrick_hand", "quicksand", "source_code_pro"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int nightMode = getSharedPreferences("theme_prefs", MODE_PRIVATE)
                .getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(nightMode);
        setContentView(R.layout.activity_note_editor);

        PDFBoxResourceLoader.init(getApplicationContext());
        db = new NoteDatabase(this);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        editContentBelow = findViewById(R.id.editContentBelow);
        TextView btnDone = findViewById(R.id.btnDone);
        ImageButton btnInfo = findViewById(R.id.btnInfo);
        ImageButton btnEditorMore = findViewById(R.id.btnEditorMore);
        ImageButton btnCopyContent = findViewById(R.id.btnCopyContent);
        TextView btnToolbarFont = findViewById(R.id.btnToolbarFont);

        // Formatting buttons
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);

        // Bottom toolbar buttons
        btnToolbarCheckbox = findViewById(R.id.btnToolbarCheckbox);
        btnToolbarBullet = findViewById(R.id.btnToolbarBullet);
        ImageButton btnToolbarMic = findViewById(R.id.btnToolbarMic);
        btnToolbarHeading = findViewById(R.id.btnToolbarHeading);
        ImageButton btnToolbarUndo = findViewById(R.id.btnToolbarUndo);
        ImageButton btnToolbarDismiss = findViewById(R.id.btnToolbarDismiss);
        checklistContainer = findViewById(R.id.checklistContainer);

        // Check if editing existing note
        noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, NEW_NOTE_ID);
        if (noteId != NEW_NOTE_ID) {
            isEditMode = true;
            loadNote(noteId);
        }

        // Done button - save and finish
        btnDone.setOnClickListener(v -> saveAndFinish());

        // Copy content button - copies only the content, not the title
        btnCopyContent.setOnClickListener(v -> {
            String content = editContent.getText().toString().trim();
            if (!content.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("note_content", content);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Content copied", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show();
            }
        });

        // Font button - opens the font picker bottom sheet
        btnToolbarFont.setOnClickListener(v -> showFontPicker());

        // Info button
        btnInfo.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show()
        );

        // More options menu
        btnEditorMore.setOnClickListener(v -> showEditorOverflowMenu(v));

        // Bottom toolbar - all "coming soon" except dismiss
        View.OnClickListener comingSoonListener = v ->
                Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();

        // Bold button - toggles bold mode for new typing (mutually exclusive with italic)
        btnBold.setOnClickListener(v -> {
            isBoldActive = !isBoldActive;
            if (isBoldActive) isItalicActive = false;
            updateStyleButtonStates();
            applyActiveStyleAtCursor();
        });

        // Italic button - toggles italic mode for new typing (mutually exclusive with bold)
        btnItalic.setOnClickListener(v -> {
            isItalicActive = !isItalicActive;
            if (isItalicActive) isBoldActive = false;
            updateStyleButtonStates();
            applyActiveStyleAtCursor();
        });

        // Checklist button - toggle adding mode on/off
        btnToolbarCheckbox.setOnClickListener(v -> {
            if (isChecklistActive) {
                // Turn off adding mode
                isChecklistActive = false;
                updateChecklistButtonState();
            } else {
                // Turn on adding mode, add a new item
                isChecklistActive = true;
                checklistContainer.setVisibility(View.VISIBLE);
                editContentBelow.setVisibility(View.VISIBLE);
                addChecklistItem("", false, true);
                updateChecklistButtonState();
            }
        });

        // When user focuses editContentBelow, turn off checklist adding mode
        editContentBelow.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isChecklistActive) {
                isChecklistActive = false;
                updateChecklistButtonState();
            }
        });
        // Bullet list button - toggle bullet mode on/off
        btnToolbarBullet.setOnClickListener(v -> {
            isBulletActive = !isBulletActive;
            updateBulletButtonState();
            if (isBulletActive) {
                EditText focused = editContentBelow.hasFocus() ? editContentBelow : editContent;
                applyBulletToCurrentLine(focused);
            } else {
                // Freeze all bullet spans so they stop growing
                freezeBulletSpans(editContent);
                freezeBulletSpans(editContentBelow);
            }
        });

        // TextWatcher to apply bullet to new lines on Enter
        editContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 1 && start < s.length() && s.charAt(start) == '\n') {
                    editContent.post(() -> {
                        if (isBulletActive) {
                            applyBulletToCurrentLine(editContent);
                        } else {
                            removeBulletFromCurrentLine(editContent);
                        }
                    });
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        editContentBelow.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 1 && start < s.length() && s.charAt(start) == '\n') {
                    editContentBelow.post(() -> {
                        if (isBulletActive) {
                            applyBulletToCurrentLine(editContentBelow);
                        } else {
                            removeBulletFromCurrentLine(editContentBelow);
                        }
                    });
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Speech-to-text button
        btnToolbarMic.setOnClickListener(v -> {
            if (!CheckPremiumStatus.isPremium) { showUpgradeDialog(); return; }
            startSpeechToText();
        });

        // Heading button - shows popup to pick H1-H6 or Normal
        btnToolbarHeading.setOnClickListener(v -> {
            showHeadingMenu(v);
        });
        // Undo button
        btnToolbarUndo.setOnClickListener(v -> {
            editContent.onTextContextMenuItem(android.R.id.undo);
        });

        // Dismiss keyboard button
        btnToolbarDismiss.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View currentFocus = getCurrentFocus();
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        });
    }

    private void loadNote(long id) {
        Note note = db.getNote(id);
        if (note != null) {
            editTitle.setText(note.getTitle());
            String fullContent = note.getContent();
            // Extract and strip the leading font marker if present
            String fontKey = "default";
            if (fullContent != null && fullContent.startsWith(FONT_MARKER_PREFIX)) {
                int end = fullContent.indexOf(FONT_MARKER_SUFFIX);
                if (end > FONT_MARKER_PREFIX.length()) {
                    fontKey = fullContent.substring(FONT_MARKER_PREFIX.length(), end);
                    fullContent = fullContent.substring(end + FONT_MARKER_SUFFIX.length());
                }
            }
            applyFont(fontKey);
            if (fullContent != null && !fullContent.isEmpty()) {
                String htmlContent;
                String checklistData = null;
                if (fullContent.contains(CHECKLIST_SEPARATOR.trim())) {
                    int sepIndex = fullContent.indexOf(CHECKLIST_SEPARATOR.trim());
                    htmlContent = fullContent.substring(0, sepIndex);
                    checklistData = fullContent.substring(sepIndex + CHECKLIST_SEPARATOR.trim().length()).trim();
                } else {
                    htmlContent = fullContent;
                }
                if (!htmlContent.trim().isEmpty()) {
                    Spanned spanned;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        spanned = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        spanned = Html.fromHtml(htmlContent);
                    }
                    editContent.setText(spanned);
                }
                if (checklistData != null && !checklistData.isEmpty()) {
                    // Check for below-checklist content
                    String belowContent = null;
                    if (checklistData.contains(BELOW_SEPARATOR.trim())) {
                        int belowIdx = checklistData.indexOf(BELOW_SEPARATOR.trim());
                        belowContent = checklistData.substring(belowIdx + BELOW_SEPARATOR.trim().length()).trim();
                        checklistData = checklistData.substring(0, belowIdx).trim();
                    }
                    loadChecklist(checklistData);
                    editContentBelow.setVisibility(View.VISIBLE);
                    if (belowContent != null && !belowContent.isEmpty()) {
                        Spanned belowSpanned;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            belowSpanned = Html.fromHtml(belowContent, Html.FROM_HTML_MODE_LEGACY);
                        } else {
                            belowSpanned = Html.fromHtml(belowContent);
                        }
                        editContentBelow.setText(belowSpanned);
                    }
                }
            }
        }
    }

    private void saveAndFinish() {
        String title = editTitle.getText().toString().trim();
        String htmlContent = getContentAsHtml().trim();
        String checklistData = serializeChecklist();
        String belowHtml = getContentBelowAsHtml().trim();

        String content;
        if (!checklistData.isEmpty()) {
            content = htmlContent + CHECKLIST_SEPARATOR + checklistData;
            if (!belowHtml.isEmpty()) {
                content += BELOW_SEPARATOR + belowHtml;
            }
        } else {
            content = htmlContent;
        }

        // Don't save completely empty notes
        boolean hasText = !title.isEmpty() || !htmlContent.isEmpty() || !belowHtml.isEmpty();
        boolean hasChecklist = !checklistData.isEmpty();
        if (!hasText && !hasChecklist) {
            finish();
            return;
        }

        // Persist selected font as a leading marker
        if (!"default".equals(selectedFontKey)) {
            content = FONT_MARKER_PREFIX + selectedFontKey + FONT_MARKER_SUFFIX + content;
        }

        Calendar c = Calendar.getInstance();
        String currentTime = pad(c.get(Calendar.HOUR)) + ":" + pad(c.get(Calendar.MINUTE));
        String amPm = c.get(Calendar.AM_PM) == Calendar.AM ? " AM" : " PM";
        currentTime += amPm;
        String todaysDate = c.get(Calendar.MONTH) + 1 + "/" + c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.YEAR);

        if (isEditMode) {
            Note note = new Note(noteId, title, content, todaysDate, currentTime);
            db.editnote(note);
        } else {
            Note note = new Note(title, content, todaysDate, currentTime);
            db.addNote(note);
        }

        finish();
    }

    private String pad(int i) {
        if (i < 10) return "0" + i;
        return String.valueOf(i);
    }

    private void addChecklistItem(String text, boolean checked, boolean focus) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_checklist, checklistContainer, false);
        CheckBox checkBox = itemView.findViewById(R.id.checklistCheckbox);
        EditText editText = itemView.findViewById(R.id.checklistText);
        ImageButton deleteBtn = itemView.findViewById(R.id.checklistDelete);

        editText.setText(text);
        checkBox.setChecked(checked);
        applyStrikethrough(editText, checked);

        // Toggle strikethrough when checked/unchecked
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> applyStrikethrough(editText, isChecked));

        // Delete this checklist item
        deleteBtn.setOnClickListener(v -> checklistContainer.removeView(itemView));

        // Enter key creates a new checklist item below
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                int index = checklistContainer.indexOfChild(itemView);
                addChecklistItemAt(index + 1, "", false, true);
                return true;
            }
            return false;
        });

        // Also handle Enter key press directly
        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                int index = checklistContainer.indexOfChild(itemView);
                addChecklistItemAt(index + 1, "", false, true);
                return true;
            }
            return false;
        });

        checklistContainer.addView(itemView);

        if (focus) {
            editText.requestFocus();
        }
    }

    private void addChecklistItemAt(int index, String text, boolean checked, boolean focus) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_checklist, checklistContainer, false);
        CheckBox checkBox = itemView.findViewById(R.id.checklistCheckbox);
        EditText editText = itemView.findViewById(R.id.checklistText);
        ImageButton deleteBtn = itemView.findViewById(R.id.checklistDelete);

        editText.setText(text);
        checkBox.setChecked(checked);
        applyStrikethrough(editText, checked);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> applyStrikethrough(editText, isChecked));

        deleteBtn.setOnClickListener(v -> checklistContainer.removeView(itemView));

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                int idx = checklistContainer.indexOfChild(itemView);
                addChecklistItemAt(idx + 1, "", false, true);
                return true;
            }
            return false;
        });

        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                int idx = checklistContainer.indexOfChild(itemView);
                addChecklistItemAt(idx + 1, "", false, true);
                return true;
            }
            return false;
        });

        checklistContainer.addView(itemView, index);

        if (focus) {
            editText.requestFocus();
        }
    }

    private void applyStrikethrough(EditText editText, boolean strike) {
        if (strike) {
            editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            editText.setAlpha(0.5f);
        } else {
            editText.setPaintFlags(editText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            editText.setAlpha(1.0f);
        }
    }

    private String serializeChecklist() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < checklistContainer.getChildCount(); i++) {
            View item = checklistContainer.getChildAt(i);
            CheckBox cb = item.findViewById(R.id.checklistCheckbox);
            EditText et = item.findViewById(R.id.checklistText);
            sb.append(cb.isChecked() ? "1" : "0");
            sb.append("|");
            sb.append(et.getText().toString());
            if (i < checklistContainer.getChildCount() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void loadChecklist(String checklistData) {
        if (checklistData == null || checklistData.isEmpty()) return;
        String[] lines = checklistData.split("\n");
        for (String line : lines) {
            if (line.length() < 2 || line.indexOf('|') < 0) continue;
            boolean checked = line.charAt(0) == '1';
            String text = line.substring(line.indexOf('|') + 1);
            addChecklistItem(text, checked, false);
        }
    }

    private String getContentAsHtml() {
        Spannable spannable = editContent.getText();
        return buildHtmlFromSpannable(spannable);
    }

    private String getContentBelowAsHtml() {
        if (editContentBelow.getVisibility() != View.VISIBLE) return "";
        Spannable spannable = editContentBelow.getText();
        return buildHtmlFromSpannable(spannable);
    }

    private String buildHtmlFromSpannable(Spannable spannable) {
        String text = spannable.toString();
        if (text.trim().isEmpty()) return "";

        String[] lines = text.split("\n", -1);
        StringBuilder html = new StringBuilder();
        int pos = 0;
        boolean inBulletList = false;

        for (String line : lines) {
            int lineStart = pos;
            int lineEnd = pos + line.length();

            boolean isBullet = hasBulletSpan(spannable, lineStart, lineEnd);
            int headingLevel = getHeadingLevelForRange(spannable, lineStart, lineEnd);

            if (isBullet) {
                if (!inBulletList) {
                    html.append("<ul>");
                    inBulletList = true;
                }
                html.append("<li>");
                buildInlineHtml(html, spannable, lineStart, lineEnd, false);
                html.append("</li>");
            } else {
                if (inBulletList) {
                    html.append("</ul>");
                    inBulletList = false;
                }
                String tag = headingLevel > 0 ? "h" + headingLevel : "p";
                html.append("<").append(tag).append(">");
                buildInlineHtml(html, spannable, lineStart, lineEnd, headingLevel > 0);
                html.append("</").append(tag).append(">");
            }

            pos = lineEnd + 1;
        }

        if (inBulletList) {
            html.append("</ul>");
        }

        return html.toString();
    }

    private boolean hasBulletSpan(Spannable spannable, int start, int end) {
        BulletSpan[] spans = spannable.getSpans(start, end, BulletSpan.class);
        for (BulletSpan span : spans) {
            int spanStart = spannable.getSpanStart(span);
            if (spanStart >= start && spanStart <= end) {
                return true;
            }
        }
        return false;
    }

    private int getHeadingLevelForRange(Spannable spannable, int start, int end) {
        if (start >= end) return 0;
        RelativeSizeSpan[] spans = spannable.getSpans(start, end, RelativeSizeSpan.class);
        for (RelativeSizeSpan span : spans) {
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            if (spanStart <= start && spanEnd >= end) {
                float size = span.getSizeChange();
                for (int level = 1; level <= 6; level++) {
                    if (Math.abs(size - HEADING_SIZES[level]) < 0.01f) {
                        return level;
                    }
                }
            }
        }
        return 0;
    }

    private void buildInlineHtml(StringBuilder html, Spannable spannable, int start, int end, boolean isHeading) {
        if (start >= end) return;

        TreeSet<Integer> transitions = new TreeSet<>();
        transitions.add(start);
        transitions.add(end);

        StyleSpan[] styleSpans = spannable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan s : styleSpans) {
            int ss = Math.max(spannable.getSpanStart(s), start);
            int se = Math.min(spannable.getSpanEnd(s), end);
            transitions.add(ss);
            transitions.add(se);
        }

        Integer[] points = transitions.toArray(new Integer[0]);

        for (int i = 0; i < points.length - 1; i++) {
            int segStart = points[i];
            int segEnd = points[i + 1];
            if (segStart >= segEnd) continue;

            boolean bold = false, italic = false;
            StyleSpan[] segs = spannable.getSpans(segStart, segEnd, StyleSpan.class);
            for (StyleSpan s : segs) {
                if (s.getStyle() == Typeface.BOLD) bold = true;
                if (s.getStyle() == Typeface.ITALIC) italic = true;
                if (s.getStyle() == Typeface.BOLD_ITALIC) { bold = true; italic = true; }
            }

            if (isHeading) bold = false;

            if (bold) html.append("<b>");
            if (italic) html.append("<i>");

            for (int j = segStart; j < segEnd; j++) {
                char c = spannable.charAt(j);
                switch (c) {
                    case '<': html.append("&lt;"); break;
                    case '>': html.append("&gt;"); break;
                    case '&': html.append("&amp;"); break;
                    default: html.append(c);
                }
            }

            if (italic) html.append("</i>");
            if (bold) html.append("</b>");
        }
    }

    private static final float[] HEADING_SIZES = {1.0f, 1.5f, 1.25f, 1.0f, 1.0f, 0.83f, 0.67f};

    private void showHeadingMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.heading_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.heading_none) {
                setHeadingLevel(0);
            } else if (id == R.id.heading_h1) {
                setHeadingLevel(1);
            } else if (id == R.id.heading_h2) {
                setHeadingLevel(2);
            } else if (id == R.id.heading_h3) {
                setHeadingLevel(3);
            } else if (id == R.id.heading_h4) {
                setHeadingLevel(4);
            } else if (id == R.id.heading_h5) {
                setHeadingLevel(5);
            } else if (id == R.id.heading_h6) {
                setHeadingLevel(6);
            }
            return true;
        });
        popup.show();
    }

    private void setHeadingLevel(int level) {
        activeHeadingLevel = level;
        if (level == 0) {
            btnToolbarHeading.setText("H");
            btnToolbarHeading.setTextColor(getResources().getColor(R.color.hintTextColor));
        } else {
            btnToolbarHeading.setText("H" + level);
            btnToolbarHeading.setTextColor(getResources().getColor(R.color.actionTextColor));
        }
        applyActiveStyleAtCursor();
    }

    private void applyBulletToCurrentLine(EditText editor) {
        int cursor = editor.getSelectionStart();
        if (cursor < 0) return;
        Spannable spannable = editor.getText();
        String text = spannable.toString();

        // Find start of current line
        int lineStart = text.lastIndexOf('\n', cursor - 1) + 1;
        // Find end of current line
        int lineEnd = text.indexOf('\n', cursor);
        if (lineEnd < 0) lineEnd = text.length();

        // Remove any BulletSpan that overlaps this line but started on a previous line
        BulletSpan[] existing = spannable.getSpans(lineStart, lineEnd, BulletSpan.class);
        for (BulletSpan span : existing) {
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            // If span started before this line, trim it to end before this line
            if (spanStart < lineStart) {
                spannable.removeSpan(span);
                if (spanStart < lineStart) {
                    spannable.setSpan(new BulletSpan(BULLET_GAP_WIDTH), spanStart, lineStart - 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                // Span starts on this line, keep it
                return;
            }
        }

        // Apply new BulletSpan to this line
        spannable.setSpan(new BulletSpan(BULLET_GAP_WIDTH), lineStart, lineEnd,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
    }

    private void freezeBulletSpans(EditText editor) {
        Spannable spannable = editor.getText();
        BulletSpan[] spans = spannable.getSpans(0, spannable.length(), BulletSpan.class);
        for (BulletSpan span : spans) {
            int flags = spannable.getSpanFlags(span);
            if (flags == Spannable.SPAN_INCLUSIVE_INCLUSIVE) {
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                spannable.removeSpan(span);
                spannable.setSpan(new BulletSpan(BULLET_GAP_WIDTH), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void removeBulletFromCurrentLine(EditText editor) {
        int cursor = editor.getSelectionStart();
        if (cursor < 0) return;
        Spannable spannable = editor.getText();
        String text = spannable.toString();

        int lineStart = text.lastIndexOf('\n', cursor - 1) + 1;
        int lineEnd = text.indexOf('\n', cursor);
        if (lineEnd < 0) lineEnd = text.length();

        BulletSpan[] spans = spannable.getSpans(lineStart, lineEnd, BulletSpan.class);
        for (BulletSpan span : spans) {
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            // If the span leaked into this line, trim it back
            if (spanStart < lineStart) {
                spannable.removeSpan(span);
                spannable.setSpan(new BulletSpan(BULLET_GAP_WIDTH), spanStart, lineStart - 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (spanStart >= lineStart) {
                // Span starts on this line - remove it entirely from this line
                spannable.removeSpan(span);
            }
        }
    }

    private void updateBulletButtonState() {
        btnToolbarBullet.setColorFilter(
                isBulletActive ?
                        getResources().getColor(R.color.actionTextColor) :
                        getResources().getColor(R.color.hintTextColor));
    }

    private void updateChecklistButtonState() {
        btnToolbarCheckbox.setColorFilter(
                isChecklistActive ?
                        getResources().getColor(R.color.actionTextColor) :
                        getResources().getColor(R.color.hintTextColor));
    }

    private void updateStyleButtonStates() {
        btnBold.setTextColor(isBoldActive ?
                getResources().getColor(R.color.actionTextColor) :
                getResources().getColor(R.color.hintTextColor));
        btnItalic.setTextColor(isItalicActive ?
                getResources().getColor(R.color.actionTextColor) :
                getResources().getColor(R.color.hintTextColor));
    }

    private void applyActiveStyleAtCursor() {
        int cursor = editContent.getSelectionStart();
        Spannable spannable = editContent.getText();

        // Remove existing INCLUSIVE StyleSpans at cursor that we manage
        StyleSpan[] existingStyle = spannable.getSpans(cursor, cursor, StyleSpan.class);
        for (StyleSpan span : existingStyle) {
            int flags = spannable.getSpanFlags(span);
            if (flags == Spannable.SPAN_INCLUSIVE_INCLUSIVE) {
                int spanStart = spannable.getSpanStart(span);
                spannable.removeSpan(span);
                if (spanStart < cursor) {
                    spannable.setSpan(new StyleSpan(span.getStyle()), spanStart, cursor,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        // Remove existing INCLUSIVE RelativeSizeSpans at cursor
        RelativeSizeSpan[] existingSize = spannable.getSpans(cursor, cursor, RelativeSizeSpan.class);
        for (RelativeSizeSpan span : existingSize) {
            int flags = spannable.getSpanFlags(span);
            if (flags == Spannable.SPAN_INCLUSIVE_INCLUSIVE) {
                int spanStart = spannable.getSpanStart(span);
                spannable.removeSpan(span);
                if (spanStart < cursor) {
                    spannable.setSpan(new RelativeSizeSpan(span.getSizeChange()), spanStart, cursor,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        // Insert zero-width spans at cursor for active styles
        if (isBoldActive || activeHeadingLevel > 0) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (isItalicActive) {
            spannable.setSpan(new StyleSpan(Typeface.ITALIC), cursor, cursor,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        // Insert heading size span if active
        if (activeHeadingLevel > 0) {
            float size = HEADING_SIZES[activeHeadingLevel];
            spannable.setSpan(new RelativeSizeSpan(size), cursor, cursor,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    private void showEditorOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.editor_overflow_menu, popup.getMenu());
        if (!CheckPremiumStatus.isPremium) {
            int[] premiumIds = {R.id.menu_export_pdf, R.id.menu_export_html, R.id.menu_export_markdown};
            for (int id : premiumIds) {
                MenuItem mi = popup.getMenu().findItem(id);
                if (mi != null) mi.setTitle(buildPremiumTitle(mi.getTitle().toString()));
            }
        }
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_delete_note) {
                if (isEditMode) {
                    showDeleteConfirmation();
                } else {
                    finish();
                }
                return true;
            } else if (id == R.id.menu_export_pdf) {
                if (!CheckPremiumStatus.isPremium) { showUpgradeDialog(); return true; }
                exportToPdf();
                return true;
            } else if (id == R.id.menu_export_text) {
                exportToText();
                return true;
            } else if (id == R.id.menu_export_html) {
                if (!CheckPremiumStatus.isPremium) { showUpgradeDialog(); return true; }
                exportToHtml();
                return true;
            } else if (id == R.id.menu_export_markdown) {
                if (!CheckPremiumStatus.isPremium) { showUpgradeDialog(); return true; }
                exportToMarkdown();
                return true;
            }
            Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
            return true;
        });
        popup.show();
    }

    private CharSequence buildPremiumTitle(String title) {
        Drawable crown = ContextCompat.getDrawable(this, R.drawable.ic_crown);
        if (crown == null) return title;
        int size = (int) (getResources().getDisplayMetrics().density * 16);
        crown.setBounds(0, 0, size, size);
        String full = title + "  \u00A0";
        SpannableString ss = new SpannableString(full);
        ss.setSpan(new ImageSpan(crown, ImageSpan.ALIGN_BASELINE),
                full.length() - 1, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    // ---------------- Font picker ----------------

    private boolean isFontFree(String key) {
        return "default".equals(key) || "inter".equals(key);
    }

    private int getFontResId(String key) {
        switch (key) {
            case "inter": return R.font.inter;
            case "caveat": return R.font.caveat;
            case "lora": return R.font.lora;
            case "merriweather": return R.font.merriweather;
            case "nunito": return R.font.nunito;
            case "patrick_hand": return R.font.patrick_hand;
            case "quicksand": return R.font.quicksand;
            case "source_code_pro": return R.font.source_code_pro;
            default: return 0;
        }
    }

    private Typeface getTypefaceForKey(String key) {
        int resId = getFontResId(key);
        if (resId == 0) return Typeface.DEFAULT;
        try {
            return ResourcesCompat.getFont(this, resId);
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    private void applyFont(String key) {
        selectedFontKey = key;
        Typeface tf = getTypefaceForKey(key);
        editContent.setTypeface(tf);
        editContentBelow.setTypeface(tf);
        editTitle.setTypeface(tf, Typeface.BOLD);
    }

    private int dpToPx(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp);
    }

    private int resolveThemeColor(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private void showFontPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        int textColor = editContent.getCurrentTextColor();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(resolveThemeColor(android.R.attr.colorBackground));
        root.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(16));

        // Header: close (start), "Font" title (center), check (end)
        FrameLayout header = new FrameLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageButton close = new ImageButton(this);
        close.setBackground(null);
        close.setImageResource(R.drawable.vector_close_24);
        close.setColorFilter(textColor);
        close.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("Font");
        headerTitle.setTextColor(textColor);
        headerTitle.setTextSize(18f);
        headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerTitle.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        header.addView(headerTitle);

        ImageButton check = new ImageButton(this);
        check.setBackground(null);
        check.setImageResource(R.drawable.ic_check_white_24dp);
        check.setColorFilter(getResources().getColor(R.color.actionTextColor));
        check.setScaleType(ImageView.ScaleType.FIT_CENTER);
        check.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams checkLp = new FrameLayout.LayoutParams(
                dpToPx(30), dpToPx(30), Gravity.END | Gravity.CENTER_VERTICAL);
        check.setLayoutParams(checkLp);
        check.setOnClickListener(v -> dialog.dismiss());
        header.addView(check);

        root.addView(header);

        // Grid of font cards (3 columns)
        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        scroll.setLayoutParams(scrollLp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setPadding(0, dpToPx(12), 0, 0);

        int columns = 3;
        int cellMargin = dpToPx(6);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cellWidth = (screenWidth - dpToPx(32) - cellMargin * 2 * columns) / columns;

        for (String key : FONT_KEYS) {
            View card = buildFontCard(key, textColor, dialog);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = cellWidth;
            glp.height = dpToPx(64);
            glp.setMargins(cellMargin, cellMargin, cellMargin, cellMargin);
            card.setLayoutParams(glp);
            grid.addView(card);
        }

        scroll.addView(grid);
        root.addView(scroll);

        dialog.setContentView(root);
        dialog.show();
    }

    private View buildFontCard(String key, int textColor, BottomSheetDialog dialog) {
        FrameLayout card = new FrameLayout(this);
        boolean selected = key.equals(selectedFontKey);
        boolean locked = !isFontFree(key) && !CheckPremiumStatus.isPremium;

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(Color.parseColor("#22808080"));
        if (selected) {
            bg.setStroke(dpToPx(2), getResources().getColor(R.color.actionTextColor));
        }
        card.setBackground(bg);

        TextView preview = new TextView(this);
        preview.setText("SimpleNotes");
        preview.setTextColor(textColor);
        preview.setTextSize(16f);
        preview.setGravity(Gravity.CENTER);
        preview.setMaxLines(1);
        preview.setTypeface(getTypefaceForKey(key));
        preview.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        card.addView(preview);

        if (locked) {
            preview.setAlpha(0.45f);
            ImageView crown = new ImageView(this);
            crown.setImageResource(R.drawable.ic_crown);
            FrameLayout.LayoutParams crownLp = new FrameLayout.LayoutParams(
                    dpToPx(16), dpToPx(16), Gravity.END | Gravity.TOP);
            crownLp.setMargins(0, dpToPx(6), dpToPx(6), 0);
            crown.setLayoutParams(crownLp);
            card.addView(crown);
        }

        card.setOnClickListener(v -> {
            if (!isFontFree(key) && !CheckPremiumStatus.isPremium) {
                showUpgradeDialog();
                return;
            }
            applyFont(key);
            dialog.dismiss();
        });

        return card;
    }

    private void showUpgradeDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Upgrade to Premium")
                .setMessage("To use this feature please upgrade to premium")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    startActivity(new Intent(this, SubscriptionActivity2.class));
                })
                .show();
    }

    private String getSanitizedFileName(String title) {
        String name = title.replaceAll("[^a-zA-Z0-9\\s-]", "").trim();
        return name.isEmpty() ? "note" : name;
    }

    private String getNoteTitle() {
        String t = editTitle.getText().toString().trim();
        return t.isEmpty() ? "Untitled" : t;
    }

    private String getPlainTextContent() {
        StringBuilder sb = new StringBuilder();
        // Content above
        String above = editContent.getText().toString();
        if (!above.trim().isEmpty()) sb.append(above);
        // Checklist
        if (checklistContainer.getChildCount() > 0) {
            if (sb.length() > 0) sb.append("\n\n");
            for (int i = 0; i < checklistContainer.getChildCount(); i++) {
                View item = checklistContainer.getChildAt(i);
                CheckBox cb = item.findViewById(R.id.checklistCheckbox);
                EditText et = item.findViewById(R.id.checklistText);
                sb.append(cb.isChecked() ? "[x] " : "[ ] ").append(et.getText().toString());
                if (i < checklistContainer.getChildCount() - 1) sb.append("\n");
            }
        }
        // Content below
        if (editContentBelow.getVisibility() == View.VISIBLE) {
            String below = editContentBelow.getText().toString();
            if (!below.trim().isEmpty()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(below);
            }
        }
        return sb.toString();
    }

    private void shareFile(File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    private void exportToText() {
        String title = getNoteTitle();
        String fileName = getSanitizedFileName(title) + ".txt";
        try {
            StringBuilder content = new StringBuilder();
            content.append(title).append("\n\n");
            content.append(getPlainTextContent());

            File exportDir = new File(getCacheDir(), "text_exports");
            if (!exportDir.exists()) exportDir.mkdirs();
            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(content.toString());
            writer.close();

            shareFile(file, "text/plain");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export text", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToHtml() {
        String title = getNoteTitle();
        String fileName = getSanitizedFileName(title) + ".html";
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html>\n<head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("<title>").append(escapeHtmlChars(title)).append("</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: -apple-system, sans-serif; padding: 20px; max-width: 700px; margin: 0 auto; }\n");
            html.append("h1 { font-size: 24px; }\n");
            html.append(".checklist { list-style: none; padding-left: 0; }\n");
            html.append(".checklist li { padding: 4px 0; }\n");
            html.append("</style>\n</head>\n<body>\n");

            // Title
            html.append("<h1>").append(escapeHtmlChars(title)).append("</h1>\n");

            // Content above
            String aboveHtml = getContentAsHtml();
            if (!aboveHtml.trim().isEmpty()) {
                html.append(aboveHtml).append("\n");
            }

            // Checklist
            if (checklistContainer.getChildCount() > 0) {
                html.append("<ul class=\"checklist\">\n");
                for (int i = 0; i < checklistContainer.getChildCount(); i++) {
                    View item = checklistContainer.getChildAt(i);
                    CheckBox cb = item.findViewById(R.id.checklistCheckbox);
                    EditText et = item.findViewById(R.id.checklistText);
                    String checked = cb.isChecked() ? " checked disabled" : " disabled";
                    html.append("<li><input type=\"checkbox\"").append(checked).append("> ");
                    html.append(escapeHtmlChars(et.getText().toString()));
                    html.append("</li>\n");
                }
                html.append("</ul>\n");
            }

            // Content below
            String belowHtml = getContentBelowAsHtml();
            if (!belowHtml.trim().isEmpty()) {
                html.append(belowHtml).append("\n");
            }

            html.append("</body>\n</html>");

            File exportDir = new File(getCacheDir(), "html_exports");
            if (!exportDir.exists()) exportDir.mkdirs();
            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(html.toString());
            writer.close();

            shareFile(file, "text/html");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export HTML", Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeHtmlChars(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void exportToMarkdown() {
        String title = getNoteTitle();
        String fileName = getSanitizedFileName(title) + ".md";
        try {
            StringBuilder md = new StringBuilder();
            md.append("# ").append(title).append("\n\n");

            // Content above - convert spannable to markdown
            Spannable aboveSpannable = editContent.getText();
            md.append(spannableToMarkdown(aboveSpannable));

            // Checklist
            if (checklistContainer.getChildCount() > 0) {
                if (md.length() > 0 && md.charAt(md.length() - 1) != '\n') md.append("\n");
                md.append("\n");
                for (int i = 0; i < checklistContainer.getChildCount(); i++) {
                    View item = checklistContainer.getChildAt(i);
                    CheckBox cb = item.findViewById(R.id.checklistCheckbox);
                    EditText et = item.findViewById(R.id.checklistText);
                    md.append(cb.isChecked() ? "- [x] " : "- [ ] ");
                    md.append(et.getText().toString()).append("\n");
                }
            }

            // Content below
            if (editContentBelow.getVisibility() == View.VISIBLE) {
                Spannable belowSpannable = editContentBelow.getText();
                String belowMd = spannableToMarkdown(belowSpannable);
                if (!belowMd.trim().isEmpty()) {
                    md.append("\n").append(belowMd);
                }
            }

            File exportDir = new File(getCacheDir(), "markdown_exports");
            if (!exportDir.exists()) exportDir.mkdirs();
            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(md.toString());
            writer.close();

            shareFile(file, "text/markdown");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export Markdown", Toast.LENGTH_SHORT).show();
        }
    }

    private String spannableToMarkdown(Spannable spannable) {
        String text = spannable.toString();
        if (text.trim().isEmpty()) return "";

        String[] lines = text.split("\n", -1);
        StringBuilder md = new StringBuilder();
        int pos = 0;

        for (String line : lines) {
            int lineStart = pos;
            int lineEnd = pos + line.length();

            boolean isBullet = hasBulletSpan(spannable, lineStart, lineEnd);
            int headingLevel = getHeadingLevelForRange(spannable, lineStart, lineEnd);

            if (headingLevel > 0) {
                for (int h = 0; h < headingLevel; h++) md.append("#");
                md.append(" ");
            } else if (isBullet) {
                md.append("- ");
            }

            // Process inline formatting segment by segment
            md.append(buildInlineMarkdown(spannable, lineStart, lineEnd));
            md.append("\n");

            pos = lineEnd + 1;
        }

        return md.toString();
    }

    private String buildInlineMarkdown(Spannable spannable, int start, int end) {
        if (start >= end) return "";

        TreeSet<Integer> transitions = new TreeSet<>();
        transitions.add(start);
        transitions.add(end);

        StyleSpan[] styleSpans = spannable.getSpans(start, end, StyleSpan.class);
        for (StyleSpan s : styleSpans) {
            int ss = Math.max(spannable.getSpanStart(s), start);
            int se = Math.min(spannable.getSpanEnd(s), end);
            transitions.add(ss);
            transitions.add(se);
        }

        Integer[] points = transitions.toArray(new Integer[0]);
        StringBuilder md = new StringBuilder();

        for (int i = 0; i < points.length - 1; i++) {
            int segStart = points[i];
            int segEnd = points[i + 1];
            if (segStart >= segEnd) continue;

            String segText = spannable.subSequence(segStart, segEnd).toString();
            boolean bold = false, italic = false;
            StyleSpan[] segStyles = spannable.getSpans(segStart, segEnd, StyleSpan.class);
            for (StyleSpan s : segStyles) {
                int ss = spannable.getSpanStart(s);
                int se = spannable.getSpanEnd(s);
                if (ss <= segStart && se >= segEnd) {
                    if (s.getStyle() == Typeface.BOLD) bold = true;
                    if (s.getStyle() == Typeface.ITALIC) italic = true;
                }
            }

            if (bold && italic) md.append("***").append(segText).append("***");
            else if (bold) md.append("**").append(segText).append("**");
            else if (italic) md.append("*").append(segText).append("*");
            else md.append(segText);
        }

        return md.toString();
    }

    private void exportToPdf() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) title = "Untitled";

        // Sanitize filename
        String fileName = title.replaceAll("[^a-zA-Z0-9\\s-]", "").trim();
        if (fileName.isEmpty()) fileName = "note";
        fileName += ".pdf";

        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50;
            float pageWidth = PDRectangle.A4.getWidth() - 2 * margin;
            float pageHeight = PDRectangle.A4.getHeight();
            float[] yPos = {pageHeight - margin}; // track y position with array for mutability
            PDPageContentStream[] cs = {new PDPageContentStream(document, page)};

            // Helper to start a new page if needed
            Runnable[] newPageHelper = new Runnable[1];
            newPageHelper[0] = () -> {
                try {
                    cs[0].endText();
                    cs[0].close();
                    PDPage newPage = new PDPage(PDRectangle.A4);
                    document.addPage(newPage);
                    cs[0] = new PDPageContentStream(document, newPage);
                    cs[0].beginText();
                    cs[0].newLineAtOffset(margin, pageHeight - margin);
                    yPos[0] = pageHeight - margin;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            cs[0].beginText();
            cs[0].newLineAtOffset(margin, yPos[0]);

            // --- Title ---
            PDFont titleFont = PDType1Font.HELVETICA_BOLD;
            float titleSize = 20;
            yPos[0] = drawWrappedText(cs, yPos, document, titleFont, titleSize, title, margin, pageWidth, pageHeight);
            // Add spacing after title
            float titleSpacing = 16;
            cs[0].newLineAtOffset(0, -titleSpacing);
            yPos[0] -= titleSpacing;

            // --- Content above checklist ---
            Spannable aboveSpannable = editContent.getText();
            yPos[0] = drawSpannableText(cs, yPos, document, aboveSpannable, margin, pageWidth, pageHeight);

            // --- Checklist ---
            if (checklistContainer.getChildCount() > 0) {
                float clSpacing = 8;
                cs[0].newLineAtOffset(0, -clSpacing);
                yPos[0] -= clSpacing;

                for (int i = 0; i < checklistContainer.getChildCount(); i++) {
                    View item = checklistContainer.getChildAt(i);
                    CheckBox cb = item.findViewById(R.id.checklistCheckbox);
                    EditText et = item.findViewById(R.id.checklistText);
                    String prefix = cb.isChecked() ? "\u2611 " : "\u2610 ";
                    String itemText = prefix + et.getText().toString();

                    PDFont font = PDType1Font.HELVETICA;
                    float fontSize = 12;
                    if (yPos[0] - fontSize < margin) {
                        newPageHelper[0].run();
                    }
                    yPos[0] = drawWrappedText(cs, yPos, document, font, fontSize, itemText, margin, pageWidth, pageHeight);
                }
            }

            // --- Content below checklist ---
            if (editContentBelow.getVisibility() == View.VISIBLE) {
                Spannable belowSpannable = editContentBelow.getText();
                String belowText = belowSpannable.toString();
                if (!belowText.trim().isEmpty()) {
                    float belowSpacing = 8;
                    cs[0].newLineAtOffset(0, -belowSpacing);
                    yPos[0] -= belowSpacing;
                    yPos[0] = drawSpannableText(cs, yPos, document, belowSpannable, margin, pageWidth, pageHeight);
                }
            }

            cs[0].endText();
            cs[0].close();

            // Save to cache
            File exportDir = new File(getCacheDir(), "pdf_exports");
            if (!exportDir.exists()) exportDir.mkdirs();
            File pdfFile = new File(exportDir, fileName);
            document.save(pdfFile);
            document.close();

            // Share
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private float drawSpannableText(PDPageContentStream[] cs, float[] yPos, PDDocument document,
                                     Spannable spannable, float margin, float pageWidth, float pageHeight) throws IOException {
        String text = spannable.toString();
        if (text.trim().isEmpty()) return yPos[0];

        String[] lines = text.split("\n", -1);
        int pos = 0;

        for (String line : lines) {
            int lineStart = pos;
            int lineEnd = pos + line.length();

            boolean isBullet = hasBulletSpan(spannable, lineStart, lineEnd);
            int headingLevel = getHeadingLevelForRange(spannable, lineStart, lineEnd);

            // Determine font and size
            float fontSize = 12;
            PDFont font = PDType1Font.HELVETICA;

            if (headingLevel > 0) {
                font = PDType1Font.HELVETICA_BOLD;
                switch (headingLevel) {
                    case 1: fontSize = 24; break;
                    case 2: fontSize = 20; break;
                    case 3: fontSize = 16; break;
                    case 4: fontSize = 14; break;
                    case 5: fontSize = 12; break;
                    case 6: fontSize = 10; break;
                }
            }

            String drawText = line;
            if (isBullet) {
                drawText = "  \u2022  " + line;
            }

            // Check for bold/italic on the entire line
            if (headingLevel == 0) {
                boolean hasBold = false;
                boolean hasItalic = false;
                StyleSpan[] styles = spannable.getSpans(lineStart, lineEnd, StyleSpan.class);
                for (StyleSpan s : styles) {
                    if (s.getStyle() == Typeface.BOLD) hasBold = true;
                    if (s.getStyle() == Typeface.ITALIC) hasItalic = true;
                }
                if (hasBold && hasItalic) font = PDType1Font.HELVETICA_BOLD_OBLIQUE;
                else if (hasBold) font = PDType1Font.HELVETICA_BOLD;
                else if (hasItalic) font = PDType1Font.HELVETICA_OBLIQUE;
            }

            if (yPos[0] - fontSize < margin) {
                // New page
                cs[0].endText();
                cs[0].close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                cs[0] = new PDPageContentStream(document, newPage);
                cs[0].beginText();
                cs[0].newLineAtOffset(margin, pageHeight - margin);
                yPos[0] = pageHeight - margin;
            }

            yPos[0] = drawWrappedText(cs, yPos, document, font, fontSize, drawText, margin, pageWidth, pageHeight);

            pos = lineEnd + 1;
        }

        return yPos[0];
    }

    private float drawWrappedText(PDPageContentStream[] cs, float[] yPos, PDDocument document,
                                   PDFont font, float fontSize, String text, float margin,
                                   float pageWidth, float pageHeight) throws IOException {
        if (text.isEmpty()) {
            // Empty line - just move down
            float lineHeight = fontSize * 1.4f;
            cs[0].newLineAtOffset(0, -lineHeight);
            yPos[0] -= lineHeight;
            return yPos[0];
        }

        // Encode-safe: replace characters PDFBox can't handle
        text = sanitizeForPdf(text);

        List<String> wrappedLines = wrapText(text, font, fontSize, pageWidth);
        float lineHeight = fontSize * 1.4f;

        for (String wLine : wrappedLines) {
            if (yPos[0] - lineHeight < margin) {
                // New page
                cs[0].endText();
                cs[0].close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                cs[0] = new PDPageContentStream(document, newPage);
                cs[0].beginText();
                cs[0].newLineAtOffset(margin, pageHeight - margin);
                yPos[0] = pageHeight - margin;
            }

            cs[0].setFont(font, fontSize);
            cs[0].newLineAtOffset(0, -lineHeight);
            yPos[0] -= lineHeight;
            cs[0].showText(wLine);
        }

        return yPos[0];
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;
            if (width > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private String sanitizeForPdf(String text) {
        // PDType1Font (Helvetica) only supports WinAnsiEncoding
        // Replace unsupported characters with safe alternatives
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\u2610': sb.append("[ ] "); break;  // ballot box
                case '\u2611': sb.append("[x] "); break;  // ballot box with check
                case '\u2612': sb.append("[x] "); break;  // ballot box with x
                case '\u2022': sb.append("-"); break;     // bullet
                default:
                    if (c < 256) {
                        sb.append(c);
                    } else {
                        sb.append(' ');
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_note))
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deleteNote(noteId);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startSpeechToText() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Could not start speech recognition: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                Toast.makeText(this, "Microphone permission is required for speech-to-text", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                int cursorPos = editContent.getSelectionStart();
                if (cursorPos < 0) cursorPos = editContent.getText().length();
                editContent.getText().insert(cursorPos, spokenText);
            }
        }
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }
}
