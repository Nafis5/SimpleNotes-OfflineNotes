package com.notes.keepnotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION=3;
    private static final String DATABASE_NAME="notedb";
    private static final String DATABASE_TABLE="notetable";
    private static final String TRASH_TABLE="trash_table";
    private static final String KEY_ID="id";
    private static final String KEY_TITLE="title";
    private static final String KEY_CONTENT="content";
    private static final String KEY_DATE="date";
    private static final String KEY_TIME="time";
    private static final String KEY_DELETED_DATE="deleted_date";
    NoteDatabase(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);

    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query="CREATE TABLE "+DATABASE_TABLE+" ("+
                KEY_ID+" INTEGER PRIMARY KEY,"+
                KEY_TITLE+" TEXT,"+
                KEY_CONTENT+" TEXT,"+
                KEY_DATE+" TEXT,"+
                KEY_TIME+" TEXT"
                +" )";
        db.execSQL(query);
        db.execSQL(createTrashTableQuery());
    }

    private String createTrashTableQuery() {
        return "CREATE TABLE "+TRASH_TABLE+" ("+
                KEY_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
                KEY_TITLE+" TEXT,"+
                KEY_CONTENT+" TEXT,"+
                KEY_DATE+" TEXT,"+
                KEY_TIME+" TEXT,"+
                KEY_DELETED_DATE+" TEXT)";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS "+TRASH_TABLE+" ("+
                    KEY_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
                    KEY_TITLE+" TEXT,"+
                    KEY_CONTENT+" TEXT,"+
                    KEY_DATE+" TEXT,"+
                    KEY_TIME+" TEXT,"+
                    KEY_DELETED_DATE+" TEXT)");
        }
    }
    public long addNote (Note note){
        SQLiteDatabase db=this.getWritableDatabase();
        ContentValues c=new ContentValues();
        if(note.getTitle().length()>0){
            c.put(KEY_TITLE,note.getTitle());
        }
        else {
            c.put(KEY_TITLE,"untitled");
        }
        c.put(KEY_CONTENT,note.getContent());
        c.put(KEY_DATE,note.getDate());
        c.put(KEY_TIME,note.getTime());
        long ID=db.insert(DATABASE_TABLE,null,c);
        return ID;
    }
    public Note getNote(long id){
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=  db.query(DATABASE_TABLE,new String[]{KEY_ID,KEY_TITLE,KEY_CONTENT,KEY_DATE,KEY_TIME},KEY_ID+"=?",new String[]{String.valueOf(id)},null,null,null,null);
        if(cursor != null)
            cursor.moveToFirst();

        Note note= new Note(Long.parseLong(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4));
       return note;
    }
    public List<Note> getAllNotes(){
        List<Note> allNotes = new ArrayList<>();
        String query = "SELECT * FROM " +DATABASE_TABLE+" ORDER BY "+KEY_ID+" DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query,null);
        if(cursor.moveToFirst()){
            do{
                Note note = new Note();
                note.setId(Long.parseLong(cursor.getString(0)));
                note.setTitle(cursor.getString(1));
                note.setContent(cursor.getString(2));
                note.setDate(cursor.getString(3));
                note.setTime(cursor.getString(4));
                allNotes.add(note);
            }while (cursor.moveToNext());
        }

        return allNotes;

    }
    void deleteNote(long id){
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor cursor=db.query(DATABASE_TABLE,
                new String[]{KEY_ID,KEY_TITLE,KEY_CONTENT,KEY_DATE,KEY_TIME},
                KEY_ID+"=?",new String[]{String.valueOf(id)},null,null,null);
        if(cursor!=null && cursor.moveToFirst()){
            ContentValues trash=new ContentValues();
            trash.put(KEY_TITLE,cursor.getString(1));
            trash.put(KEY_CONTENT,cursor.getString(2));
            trash.put(KEY_DATE,cursor.getString(3));
            trash.put(KEY_TIME,cursor.getString(4));
            trash.put(KEY_DELETED_DATE,todayString());
            db.insert(TRASH_TABLE,null,trash);
            cursor.close();
        }
        db.delete(DATABASE_TABLE,KEY_ID+"=?",new String[]{String.valueOf(id)});
        db.close();
    }
    public List<DeletedNote> getDeletedNotes(){
        purgeOldDeletedNotes();
        List<DeletedNote> list=new ArrayList<>();
        SQLiteDatabase db=this.getReadableDatabase();
        Cursor cursor=db.rawQuery("SELECT * FROM "+TRASH_TABLE+" ORDER BY "+KEY_ID+" DESC",null);
        if(cursor.moveToFirst()){
            do{
                DeletedNote n=new DeletedNote();
                n.setId(cursor.getLong(0));
                n.setTitle(cursor.getString(1));
                n.setContent(cursor.getString(2));
                n.setDate(cursor.getString(3));
                n.setTime(cursor.getString(4));
                n.setDeletedDate(cursor.getString(5));
                list.add(n);
            }while(cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void restoreNote(long trashId){
        SQLiteDatabase db=this.getWritableDatabase();
        Cursor cursor=db.query(TRASH_TABLE,
                new String[]{KEY_ID,KEY_TITLE,KEY_CONTENT,KEY_DATE,KEY_TIME},
                KEY_ID+"=?",new String[]{String.valueOf(trashId)},null,null,null);
        if(cursor!=null && cursor.moveToFirst()){
            ContentValues c=new ContentValues();
            String title=cursor.getString(1);
            c.put(KEY_TITLE,title.isEmpty()?"Untitled":title);
            c.put(KEY_CONTENT,cursor.getString(2));
            c.put(KEY_DATE,cursor.getString(3));
            c.put(KEY_TIME,cursor.getString(4));
            db.insert(DATABASE_TABLE,null,c);
            cursor.close();
        }
        db.delete(TRASH_TABLE,KEY_ID+"=?",new String[]{String.valueOf(trashId)});
        db.close();
    }

    public void permanentlyDeleteNote(long trashId){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(TRASH_TABLE,KEY_ID+"=?",new String[]{String.valueOf(trashId)});
        db.close();
    }

    public void purgeOldDeletedNotes(){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(TRASH_TABLE,KEY_DELETED_DATE+"<=?",new String[]{daysAgoString(30)});
        db.close();
    }

    private String todayString(){
        return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
    }

    private String daysAgoString(int days){
        long ms=System.currentTimeMillis()-(long)days*24*60*60*1000;
        return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(ms));
    }
    void deleteAllNotes(){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(DATABASE_TABLE,null,null);
        db.close();
    }
    void deleteAllTrashNotes(){
        SQLiteDatabase db=this.getWritableDatabase();
        db.delete(TRASH_TABLE,null,null);
        db.close();
    }
    public void editnote(Note note){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues c = new ContentValues();

       if(note.getTitle().length()>0) c.put(KEY_TITLE,note.getTitle());
       else c.put(KEY_TITLE,"untitled");
        c.put(KEY_CONTENT,note.getContent());
        c.put(KEY_DATE,note.getDate());
        c.put(KEY_TIME,note.getTime());
        db.update(DATABASE_TABLE,c,KEY_ID+"=?",new String[]{String.valueOf(note.getId())});

    }
}