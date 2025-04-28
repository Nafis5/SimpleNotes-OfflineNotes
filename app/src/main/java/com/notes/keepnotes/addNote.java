package com.notes.keepnotes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;

import java.util.Calendar;

public class addNote extends AppCompatActivity {
    Toolbar toolbar;
    EditText noteTitle,noteDetails;
    Calendar c;
    String todaysDate;
    String currentTime;
    InterstitialAd adi;
    AdManager adManager;
    private AdView mAdView;
    AdManager admanager;
    AdRequest adRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HandleNightMode();
        setContentView(R.layout.activity_add_note);


        admanager=new AdManager(this);

        admanager.loadInterstial();
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        noteTitle=findViewById(R.id.noteTitle);
        noteDetails=findViewById(R.id.noteDetails);
       // toolbar.setBackgroundColor(Color.parseColor("#000000"));
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      //  toolbar.setTitleTextColor(Color.parseColor("#000000"));
        //banner add stufss
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //banner stuff end here

        noteTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()!=0){
                    //getSupportActionBar().setTitle(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //get time and date
        c=Calendar.getInstance();

        currentTime=pad(c.get(Calendar.HOUR))+":"+pad(c.get(Calendar.MINUTE));
        todaysDate=c.get(Calendar.DAY_OF_MONTH) + "/" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "/" + Calendar.getInstance().get(Calendar.YEAR);

         Log.d("calender","Date and Time"+todaysDate+"and"+currentTime);
        noteTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP,TextSizeUtil.getEditTextSizeBasedOnScreen(this));
        noteDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP,TextSizeUtil.getEditTextSizeBasedOnScreen(this));


    }
    private void HandleNightMode() {
        CommonAttributes commonAttributes=new CommonAttributes();
        if(commonAttributes.getThemeStatus()){
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
           // getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private String pad(int i){
        if(i<10) return "0"+i;
        return String.valueOf(i);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.save_menu,menu);
        return  true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId()==R.id.save) {

            if(noteTitle.getText().toString().length()>0 || noteDetails.getText().toString().length()>0 ) {

                Note note = new Note(noteTitle.getText().toString(), noteDetails.getText().toString(), todaysDate, currentTime);
                NoteDatabase db = new NoteDatabase(this);
                db.addNote(note);
                Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();

            }
            else{
                Toast.makeText(this,"Empty notes can't be saved",Toast.LENGTH_SHORT).show();
            }
            goToMain();

        }
        if(item.getItemId()==R.id.only_delete){
            Note note = new Note(noteTitle.getText().toString(), noteDetails.getText().toString(), todaysDate, currentTime);
            NoteDatabase db = new NoteDatabase(this);
            db.deleteNote(note.getId());
            goToMain();
        }

        return  super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    public void goToMain(){
        Intent i=new Intent(this,MainActivity.class);
        i.putExtra("adDekhabo?",true);
        startActivity(i);
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();
        admanager.loadInterstial();


    }

    @Override
    protected void onStart() {
        super.onStart();

        admanager.loadInterstial();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mAdView.loadAd(adRequest);
        admanager.loadInterstial();

    }
}