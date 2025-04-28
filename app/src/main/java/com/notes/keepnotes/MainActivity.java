package com.notes.keepnotes;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.navigation.NavigationView;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener {
    boolean is_in_action_mode=false;
    int counter;
    Toolbar toolbar;
    RecyclerView recylerview;
    Adapter adapter;
    List<Note> notes;
    List<Long> deletelist=new ArrayList();
    NoteDatabase db;
    AdManager admanager;
    private FirebaseAnalytics mFirebaseAnalytics;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private MyBackupAgent backupAgent;
    private FirebaseAuth mAuth;
    String uid;
    AppOpenCounter appOpenCounter;
    private SharedPreferences sharedPreferences;
    private int subscriptionCounter;
    String WelcomeContent="Welcome, dear users! \uD83C\uDF1F Let's make note-taking a breeze. Tap \uD83D\uDDD1️ icon to remove notes. Start a new note with the ➕ icon on the Home Screen, and save it with a ✔️.\n" +
            "\n" +
            "Here's a handy trick: To quickly delete multiple notes, just press and hold on the main screen. We're here to make note-taking simple for everyone. Enjoy!\n" +
            "\n" +
            "Feel free to remove this Welcome note when you're ready.\" \uD83D\uDCDD\uD83D\uDE80\uD83D\uDC4B";
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    MenuInflater inflater;
    public NavigationView nav_view;
    private SharedPreferences darkThemesharedPreferences;
    SwitchCompat mySwitch;
    RelativeLayout PremiumBannerLayout;
    private AdView mAdView;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appOpenCounter = new AppOpenCounter(this);
        AppOpenDateManager appOpenDateManager=new AppOpenDateManager();
        int appOpenNumber = appOpenCounter.getAppOpenNumber();

        sharedPreferences = getSharedPreferences("subsPref", Context.MODE_PRIVATE);
        subscriptionCounter = sharedPreferences.getInt("SUBSCRIPTION_COUNTER_KEY", 0);

        AppOpenDateManager.setFirstOpenDate(this);



        admanager=new AdManager(this);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        db=new NoteDatabase(this);
        List<Note> notesTemp=db.getAllNotes();
    /*     if(notesTemp.size() >= 2){

             admanager.setAdShowPermission(true);
         }
         else admanager.setAdShowPermission(false);*/
       if(appOpenNumber>=2){
         //  Toast.makeText(this,"permisssion dise",Toast.LENGTH_LONG).show();
            admanager.setAdShowPermission(true);

        }
        else{
            admanager.setAdShowPermission(false);
        }


        Intent intent = getIntent();
        if(intent.getBooleanExtra("adDekhabo?", false)){
            showInterstial();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadBannerAd();
                }
            }, 30000); // 60000 milliseconds = 1 minute

        }
        else{
         //   goToSubscription();
            loadBannerAd();



        }



        admanager.loadInterstial();
        mAuth = FirebaseAuth.getInstance();
        signInUserAnonymously();
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
       // toolbar.setBackgroundColor(Color.parseColor("#000000"));
        toolbar.setTitleTextColor(getResources().getColor(R.color.TextColor));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        drawerLayout = findViewById(R.id.my_drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.nav_open, R.string.nav_close);
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.buttonColor));

        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        nav_view = (NavigationView) findViewById(R.id.nav_view);
        PremiumBannerLayout=findViewById(R.id.PremiumBanner);
        if(appOpenNumber>1 & appOpenNumber<6){
          //  PremiumBannerLayout.setVisibility(View.VISIBLE);
        }


        darkThemesharedPreferences = getSharedPreferences("darkTheme", Context.MODE_PRIVATE);





        // to make the Navigation drawer icon always appear on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recylerview=findViewById(R.id.allNotesList);
        recylerview.setLayoutManager(new LinearLayoutManager(this));
       // db=new NoteDatabase(this);
        if(appOpenNumber==0){
            Calendar c= Calendar.getInstance();

            String currentTime=pad(c.get(Calendar.HOUR))+":"+pad(c.get(Calendar.MINUTE));
            String todaysDate=c.getInstance().get(Calendar.DAY_OF_MONTH) + "/" + (Calendar.getInstance().get(Calendar.MONTH) + 1) + "/" + Calendar.getInstance().get(Calendar.YEAR);
            db.addNote(new Note("Welcome",WelcomeContent,todaysDate,currentTime));
        }

        notes=db.getAllNotes();
        adapter=new Adapter(this,notes);
        recylerview.setAdapter(adapter);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        int menuItemIndex = 3;
        MenuItem menuItem = nav_view.getMenu().getItem(menuItemIndex);
        menuItem.setActionView(R.layout.dark_theme_switch);
        mySwitch = menuItem.getActionView().findViewById(R.id.switchDarkTheme);
        if(getThemeState()) mySwitch.setChecked(true);
        if (mySwitch != null) {
            // Set the listener for the Switch

            mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Handle the Switch state change here
                    if (isChecked) {
                        ActivateNightMode();
                       // goToPremium();
                    } else {
                        DeactivateNightMode();
                       // goToPremium();
                    }
                }
            });
        }
        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if(id ==R.id.RemoveAdItem){
                    goToPremium();
                }

                if(id==R.id.backupId){
                   // Toast.makeText(MainActivity.this,"backup",Toast.LENGTH_SHORT).show();
                    goToPremium();
                }
                if(id==R.id.restoreId){
                  //  Toast.makeText(MainActivity.this,"restore",Toast.LENGTH_SHORT).show();
                    goToPremium();
                }

                return true;
            }
        });

       setCommonAttributes();
        gatherUserConsent(this);

    }
    private void loadBannerAd(){
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
    void goToPremium(){
        if (isInternetConnected(MainActivity.this)) {
            backUpData();
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "buyNow_button");
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Subscription button clicked"); // Replace with your button's name
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button_click");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            try {
                Thread.sleep(1000); // Sleep for 1500 milliseconds (1.5 seconds)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://galaxystore.samsung.com/detail/com.pro.keepnotes"));
            startActivity(intent);


        }
        else{
            Toast.makeText(this,"Please connect to the internet",Toast.LENGTH_SHORT).show();
        }
    }
    public static boolean isInternetConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    void handleSwitchClick(boolean isChecked){

        boolean isSwtichOn=darkThemesharedPreferences.getBoolean("isDarkThemeOn",false);
        if(isChecked){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            SharedPreferences.Editor editor = darkThemesharedPreferences.edit();
            editor.putBoolean("isDarkThemeOn",true);
            editor.apply();

        }
        else{


            //turn off darktheme
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            SharedPreferences.Editor editor = darkThemesharedPreferences.edit();
            editor.putBoolean("isDarkThemeOn",false);
            editor.apply();
        }

    }
    private String pad(int i){
        if(i<10) return "0"+i;
        return String.valueOf(i);

    }
    void goToSubscription(){
     /*   Intent intent = getIntent();
        boolean isFromSubscription=intent.getBooleanExtra("ComingFromSubscription",false);
        int appOpenNumber=appOpenCounter.getAppOpenNumber();
        if(appOpenNumber==0 & !isFromSubscription & subscriptionCounter<3 ){
            subscriptionCounter++;


            // Save the updated counter in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("SUBSCRIPTION_COUNTER_KEY", subscriptionCounter);
            editor.apply();

           // Toast.makeText(this,""+appOpenNumber,Toast.LENGTH_LONG).show();
            startActivity(new Intent(MainActivity.this,Subscription.class));


        }*/

    }

    void signInUserAnonymously() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInAnonymously: success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            uid = mAuth.getCurrentUser().getUid();
                            // updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInAnonymously: failure", task.getException());
                           // Toast.makeText(MainActivity.this, "Authentication failed.",
                                 //   Toast.LENGTH_SHORT).show();
                            // updateUI(null);
                        }
                    });
        } else {
            uid = currentUser.getUid();
        }
    }
    void backUpData(){
        DataTransfer dataTransfer = new DataTransfer(getApplicationContext(),MainActivity.this);


        if( !dataTransfer.isBackupAvailable() ){
            if(checkPermissions() & notes.size()!=0) dataTransfer.exportDataAsCSV((ArrayList<Note>) db.getAllNotes(), "notes_data");


        }


    }
    void importData(){
        DataTransfer dataTransfer = new DataTransfer(getApplicationContext(),MainActivity.this);
        if( dataTransfer.isBackupAvailable() & notes.size()==0 & checkPermissions()){
            List<Note> importedNotes = dataTransfer.importDataFromCSV("notes_data");

            for(Note note : importedNotes){
                db.addNote(note);


            }
            adapter.updateAdapter(importedNotes);

        }

    }

    private boolean checkPermissions() {
        // Check if both read and write permissions are already granted
        int readPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        // Request both read and write permissions
        String[] permissions = new String[]{
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

     /*   if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[1])) {
            // Display a rationale if user has denied the permissions previously
            Toast.makeText(this, "Storage permissions are required to read and write files.", Toast.LENGTH_LONG).show();
        } else {
            // Request the permissions
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }*/
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed with file operations.

            } else {
                // Permission denied. Handle the situation accordingly.
                Toast.makeText(this, "Storage permissions are required to read and write files. App cannot proceed.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showInterstial(){
        InterstitialAd mInterstial=admanager.getad(false);
        if(mInterstial!=null ){
          //  if(AdManager.isFiveMinutesPassed()){
                mInterstial.show(this);
           // }



        }else{
           // goToSubscription();
            loadBannerAd();
            admanager.loadInterstial();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.add_menu,menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {


            return true;
        }
        if(item.getItemId()==R.id.add){



            Intent i=new Intent(this,addNote.class);
            startActivity(i);

        }
        if(item.getItemId()==R.id.only_delete){
            //revome selected items from db
            //dishablehome buton
            //set title to keep notes
            //bring back old menu
            //updateAdapter

            for(int i=0;i<deletelist.size();i++){

                db.deleteNote(deletelist.get(i));
            }
            Toast.makeText(this,counter+" items deleted",Toast.LENGTH_SHORT).show();
            clearActionMode();

            notes=db.getAllNotes();
            adapter.updateAdapter(notes);





        }
        else if(item.getItemId()==android.R.id.home){

            clearActionMode();
        }
        // return  super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public boolean onLongClick(View v) {
        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.only_delete_menu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle("0 item selected");
        is_in_action_mode=true;

        adapter.notifyDataSetChanged();
        return true;
    }
    public void prepareSelection(View view,long id){
        if(((CheckBox)view).isChecked()){
            deletelist.add(id);
            counter++;
            updatecounter();
        }
        else{
            deletelist.remove(new Long(id));
            --counter;
            updatecounter();
        }
    }
    public void updatecounter(){
        if(counter==0) toolbar.setTitle("0 item selected");
        else {
            toolbar.setTitle(counter+" item selected");
        }
    }
    public  void clearActionMode(){
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        toolbar.setTitle("");

        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.add_menu);
        counter=0;
        is_in_action_mode=false;
        adapter.notifyDataSetChanged();
        deletelist.clear();

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        finishAffinity();
        System.exit(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        admanager.loadInterstial();
        appOpenCounter.incrementAppOpen();
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();
        admanager.loadInterstial();


    }

    @Override
    protected void onRestart() {
        super.onRestart();
        admanager.loadInterstial();
        loadBannerAd();
    }

    private void setCommonAttributes() {
        CommonAttributes ThemeAttribute=new CommonAttributes();
        ThemeAttribute.setDarkThemeStatus(getThemeState());
    }

    private void saveThemeState(boolean isDarkThemeOn) {
        SharedPreferences.Editor editor = darkThemesharedPreferences.edit();
        editor.putBoolean("isDarkThemeOn", isDarkThemeOn);
        editor.apply();
    }
    private boolean getThemeState() {
        return darkThemesharedPreferences.getBoolean("isDarkThemeOn", false);
    }
    void ActivateNightMode(){
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        saveThemeState(true);
        CommonAttributes commonAttributes=new CommonAttributes();
        commonAttributes.setDarkThemeStatus(true);
    }
    void DeactivateNightMode(){
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        saveThemeState(false);
        CommonAttributes commonAttributes=new CommonAttributes();
        commonAttributes.setDarkThemeStatus(false);

    }


    @Override
    protected void onResume() {
        super.onResume();
        // Apply the saved theme state when the activity is resumed
        admanager.loadInterstial();
        boolean isDarkThemeOn = getThemeState();
        if(isDarkThemeOn){
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        }
        else{
            // getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public void ViewPremiumClick(View view) {
        goToPremium();
    }

    public void onCrossPremiumClick(View view) {
        PremiumBannerLayout.setVisibility(View.GONE);

    }
    public void gatherUserConsent(Activity activity) {
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(activity, initializationStatus -> {
            // Mobile Ads SDK initialized
        });

        // Create consent request parameters
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        // Request consent information
        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        activity,
                        formError -> {
                            // Consent has been gathered, load the banner ad
                            if (formError == null) {
                                loadBannerAd();
                            } else {
                                // Handle form display error
                                //  Toast.makeText(activity, "Error displaying consent form: " + formError.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                ),
                requestConsentError -> {
                    // Handle error in requesting consent information
                    //  Toast.makeText(activity, "Error updating consent information: " + requestConsentError.getMessage(), Toast.LENGTH_LONG).show();
                }
        );
    }
}