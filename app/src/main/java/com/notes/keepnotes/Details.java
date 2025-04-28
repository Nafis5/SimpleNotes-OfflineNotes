package com.notes.keepnotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class Details extends AppCompatActivity {
    Toolbar toolbar;
    TextView detailstext;
    NoteDatabase db;
    Note note;
    InterstitialAd adi;
    private AdView mAdView;
    private AdManager adManager;
    AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HandleNightMode();
        setContentView(R.layout.activity_details);
        adManager = new AdManager(this);
        adManager.loadInterstial();
        adi = adManager.getad(true);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView2);

        showInterstial();




        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
       // toolbar.setBackgroundColor(Color.parseColor("#000000"));
        toolbar.setTitleTextColor(getResources().getColor(R.color.TextColor));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        detailstext = findViewById(R.id.detailsOfNote);
        builder = new AlertDialog.Builder(this);

        Intent i = getIntent();
        Long id = i.getLongExtra("ID", 0);
        //Toast.makeText(this,""+id,Toast.LENGTH_SHORT).show();
        db = new NoteDatabase(this);
        note = db.getNote(id);
        getSupportActionBar().setTitle(note.getTitle());
        detailstext.setText(note.getContent());
        detailstext.setMovementMethod(new ScrollingMovementMethod());
        detailstext.setTextIsSelectable(true);

        //banner add stufss


        Handler handler = new Handler(Looper.getMainLooper());
        if(adManager.getad(true) !=null ){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadBannerAd();
                }
            }, 40000); // 60000 milliseconds = 1 minute

        }
        else{

                loadBannerAd();
        }




        //banner stuff end here

    }
    private void HandleNightMode() {
        CommonAttributes commonAttributes=new CommonAttributes();
        if(commonAttributes.getThemeStatus()){

            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            //  getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    private void loadBannerAd(){
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){

            finish();
            return true;
        }
        if (item.getItemId() == R.id.deleteNote) {
            OpenDialogue();



        }
        if (item.getItemId() == R.id.editNote) {

            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            Intent i = new Intent(this, Edit.class);
            i.putExtra("ID", note.getId());
            startActivity(i);

        }

        return super.onOptionsItemSelected(item);
    }

    public void goToMain() {
        Intent i=new Intent(this,MainActivity.class);
      //  i.putExtra("adDekhabo?",true);
        startActivity(i);
    }



    void OpenDialogue() {
       // builder.setMessage(R.string.dialog_message).setTitle("delete");

        //Setting message manually and performing action on button click
        builder.setMessage("Do you want to delete this note ?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                        db.deleteNote(note.getId());
                        goToMain();



                        Toast.makeText(getApplicationContext(), "Note Deleted",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'NO' Button
                        dialog.cancel();

                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();

        alert.setTitle("Delete");
        alert.show();
    }
    private void showInterstial() {
        if (adi != null) {
          //  if(AdManager.isFiveMinutesPassed()){
                adi.show(this);


           // }



        } else {
            loadBannerAd();
            adManager.loadInterstial();

        }

    }

    @Override
    public void onBackPressed() {
        goToMain();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        loadBannerAd();
        adManager.loadInterstial();
    }

    @Override
    protected void onStart() {
        super.onStart();


    }

}



