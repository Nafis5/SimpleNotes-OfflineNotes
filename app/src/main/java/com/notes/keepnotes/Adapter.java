package com.notes.keepnotes;

import java.util.List;

public class Adapter {

    private NoteAdapter noteAdapter;

    public Adapter() {
    }

    public Adapter(NoteAdapter noteAdapter) {
        this.noteAdapter = noteAdapter;
    }

    public void updateAdapter(List<Note> notes) {
        if (noteAdapter != null) {
            noteAdapter.updateNotes(notes);
        }
    }
}
