package com.notes.keepnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;

import java.util.Calendar;

public class Edit extends AppCompatActivity {
    Toolbar toolbar;
    EditText noteTitle,noteDetails;
    Calendar c;
    String todaysDate;
    String currentTime;
    NoteDatabase db;
    Note note;
    private AdView mAdView;
    AdManager admanager;
    AdRequest adRequest;
   // private static final String PREFS_NAME = "MyPrefsFile";
  //  private static final String CLICK_COUNT_KEY = "clickCount";
 //   private SharedPreferences sharedPreferences;
    AlertDialog.Builder builder;

    InterstitialAd adi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HandleNightMode();
        setContentView(R.layout.activity_edit);


        admanager=new AdManager(this);


      //  showInterstial();

      //  sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Intent i=getIntent();
        Long id=i.getLongExtra("ID",0);
        db=new NoteDatabase(this);
        note=db.getNote(id);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        noteTitle=findViewById(R.id.noteTitle);
        noteDetails=findViewById(R.id.noteDetails);
      //  toolbar.setBackgroundColor(Color.parseColor("#000000"));
        getSupportActionBar().setTitle("");

        //toolbar.setTitleTextColor(Color.parseColor("#FFF"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        noteTitle.setText(note.getTitle());

        noteDetails.setText(note.getContent());
        c=Calendar.getInstance();
        todaysDate =c.get(Calendar.DAY_OF_MONTH) + "/" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "/" + Calendar.getInstance().get(Calendar.YEAR);

        currentTime=pad(c.get(Calendar.HOUR))+":"+pad(c.get(Calendar.MINUTE));







        builder = new AlertDialog.Builder(this);
        noteTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP,TextSizeUtil.getEditTextSizeBasedOnScreen(this));
        noteDetails.setTextSize(TypedValue.COMPLEX_UNIT_SP,TextSizeUtil.getEditTextSizeBasedOnScreen(this));
        admanager.loadInterstial();
     //   Toast.makeText(this,""+TextSizeUtil.getEditTextSizeBasedOnScreen(this),Toast.LENGTH_LONG).show();
       MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);

        loadBannerAd();
        admanager.loadInterstial();


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
  /*  private void increaseClickCounter(){
        int currentClickCount = sharedPreferences.getInt(CLICK_COUNT_KEY, 0);
        currentClickCount++;
        sharedPreferences.edit().putInt(CLICK_COUNT_KEY, currentClickCount).apply();
    }
    private int getClickCounter(){
        return sharedPreferences.getInt(CLICK_COUNT_KEY, 0);
    }*/



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.save_menu,menu);
        return  true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId()==R.id.save) {
         //   increaseClickCounter();
            if(noteTitle.getText().toString().length()>0 || noteDetails.getText().toString().length()>0 ){
                note.setTitle(noteTitle.getText().toString());
                note.setContent(noteDetails.getText().toString());
                note.setDate(todaysDate);
                note.setTime(currentTime);
                db.editnote(note);
                Toast.makeText(this,"Note updated",Toast.LENGTH_SHORT).show();

            }
            else {
                db.deleteNote(note.getId());
                Toast.makeText(this,"Empty notes can't be saved",Toast.LENGTH_SHORT).show();
            }
            goToMain();

        }
        if(item.getItemId()==R.id.only_delete){
            OpenDialogue();

        }
        if(item.getItemId() == android.R.id.home){
          //  increaseClickCounter();
            finish();

         //   goToMain();
            return true;
        }

        return  super.onOptionsItemSelected(item);
    }
    public void goToMain(){
        Intent i=new Intent(this,MainActivity.class);
      //  if( (getClickCounter())%3==0) i.putExtra("adDekhabo?",true);
      //  else  i.putExtra("adDekhabo?",false);
        i.putExtra("adDekhabo?",true);
        startActivity(i);
    }
    private String pad(int i){
        if(i<10) return "0"+i;
        return String.valueOf(i);

    }



    @Override
    protected void onRestart() {
        super.onRestart();
        loadBannerAd();
        admanager.loadInterstial();
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
    @Override
    public void onBackPressed() {
       // increaseClickCounter();
        super.onBackPressed();

    }
    void setEditTextSizeBasedOnScreen(Context context, EditText editText) {
        // Get screen dimensions
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        // Determine screen size category
        int screenSize = context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        // Define default text size and adjust based on screen size
        float defaultTextSize = 18; // default size in sp

        switch (screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize - 2);
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize);
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize + 2);
                break;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize + 4);
                break;
            default:
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultTextSize);
        }
    }
    private void showInterstial(){
        InterstitialAd mInterstial= admanager.getad(false);
        if(mInterstial!=null ){
           if( AdManager.isFiveMinutesPassed())  mInterstial.show(this);





        }else{
            // goToSubscription();
            admanager.loadInterstial();
        }


    }

}
