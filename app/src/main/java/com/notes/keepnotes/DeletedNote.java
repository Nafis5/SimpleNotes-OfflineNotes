package com.notes.keepnotes;

public class DeletedNote {
    private long id;
    private String title;
    private String content;
    private String date;
    private String time;
    private String deletedDate;

    public DeletedNote() {}

    public DeletedNote(long id, String title, String content, String date, String time, String deletedDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.time = time;
        this.deletedDate = deletedDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDeletedDate() { return deletedDate; }
    public void setDeletedDate(String deletedDate) { this.deletedDate = deletedDate; }
}
