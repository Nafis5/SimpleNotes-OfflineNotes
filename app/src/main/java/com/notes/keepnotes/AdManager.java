package com.notes.keepnotes;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Calendar;

public class AdManager {
    static InterstitialAd lowInterstitialAd;
    static InterstitialAd highInterstitialAd;
    private Context ctx;
    private static boolean adShowPermission=true;
    private static long  AdUnit1LastCalledTime;
    private static long AdUnit2LastCalledTime;
    private static long AdUnit3LastCalledTime;
    private static long AdUnit4LastCalledTime;
     static boolean ad1IsLoading=false;
     static boolean ad2IsLoading=false;
     static boolean ad3IsLoading=false;
     static boolean ad4IsLoading=false;
    private static int lastAdUnitCallDay = -1;
    private static long lastAdShownTime = 0;
    private static final long FIVE_MINUTES_IN_MILLIS = 2 * 60 * 1000; // 5 minutes in milliseconds



    private  final String highAdunit1="ca-app-pub-3103198316569371/5239083224";
    private  final String highAdunit2="ca-app-pub-3103198316569371/1443308440";
    private  final String highAdunit3="ca-app-pub-3103198316569371/9612522634";
    private  final String highAdunit4="ca-app-pub-3103198316569371/1108266522";

    //below are test ad unitsprivate
   /* private final String highAdunit1="ca-app-pub-3940256099942544/8691691433";
    private  final String highAdunit2="ca-app-pub-3940256099942544/8691691433";
    private  final String highAdunit3="ca-app-pub-3940256099942544/8691691433";
    private  final String highAdunit4="ca-app-pub-3940256099942544/1033173712";*/


    static InterstitialAd interstitialAd1;
    static InterstitialAd interstitialAd2;
    static InterstitialAd interstitialAd3;
    static InterstitialAd interstitialAd4;

    public AdManager(Context ctx) {
        this.ctx = ctx;


    }
    void setAdShowPermission(boolean adShowPermission){
        this.adShowPermission=adShowPermission;
    }

    /* public static InterstitialAd getad() {
         if (highInterstitialAd != null) return highInterstitialAd;
         return lowInterstitialAd;
     }*/
    public static InterstitialAd getad(boolean isForDetails){
        if(interstitialAd1 !=null) return interstitialAd1;
        else if(interstitialAd2 !=null) return interstitialAd2;
        else if(interstitialAd3 !=null) return interstitialAd3;
        else{
            if( !isFiveMinutesPassed() || isForDetails ) return null;
        //    if( !isFiveMinutesPassed()  ) return null;
            else return interstitialAd4;
        }




    }


    private static void updatelastAdShownTime() {
        lastAdShownTime = System.currentTimeMillis();
    }
    public static boolean isFiveMinutesPassed() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastAdShownTime;

        // Check if 5 minutes have passed since the last method call
        boolean isFiveMinutesPassed = elapsedTime >= FIVE_MINUTES_IN_MILLIS;

        if (isFiveMinutesPassed) {
            Log.d("TimeManager", "Five minutes have passed since the last method call.");
        }

        return isFiveMinutesPassed;
    }


    public void loadInterstial() {
        //   if(highInterstitialAd==null) loadAdHigh();
        // if(lowInterstitialAd==null) loadAdLow();
        //  setYesterday(); //remove this line
         setAdsNullIfExpired();
        loadAds1(highAdunit1);
        if(  isAllHighAdsNull()  & adShowPermission  ) {


            loadAds2(highAdunit2);
            loadAds3(highAdunit3);
            if (interstitialAd4==null){
                Log.d("adsPrint", "low ecpm load hoite disi");
                loadAds4(highAdunit4);
            }
            Calendar calendar = Calendar.getInstance();
            lastAdUnitCallDay=calendar.get(Calendar.DAY_OF_YEAR);


        }

    }





        /*else{
           String msg="";
            if(isAllHighAdsNull()) msg=msg+"all null: ";
            else msg=msg+"all filled:";

            Toast.makeText(ctx,msg,Toast.LENGTH_LONG).show();
        }*/




    void setAdsNullIfExpired(){
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
        if( currentDay!=lastAdUnitCallDay & lastAdUnitCallDay!=-1){
            setAllAdsNUll();


            return;
        }
        if(hasAdunitExpired(AdUnit1LastCalledTime)){
            interstitialAd1=null;

        }
        if(hasAdunitExpired(AdUnit2LastCalledTime)) interstitialAd2=null;
        if(hasAdunitExpired(AdUnit3LastCalledTime)) interstitialAd3=null;
        if(hasAdunitExpired(AdUnit4LastCalledTime)) interstitialAd4=null;

    }
    void setAllAdsNUll(){
        interstitialAd1=null;
        interstitialAd2=null;
        interstitialAd3=null;
        interstitialAd4=null;
    }
    public boolean hasAllHighAdsexpired(){
        return hasAdunitExpired(AdUnit1LastCalledTime) & hasAdunitExpired(AdUnit2LastCalledTime) & hasAdunitExpired(AdUnit3LastCalledTime) ;
    }
    public boolean hasAdunitExpired(long AdUnitLastCalledTime) {
        if(AdUnitLastCalledTime==0) return false;
        long currentTime = System.currentTimeMillis();
        long fiftyMinutesInMillis = 50 * 60 * 1000; // 10 minutes in milliseconds


        // Check if the difference between the current time and the last called time is less than 10 minutes
        return (Math.abs(currentTime - AdUnitLastCalledTime)) > fiftyMinutesInMillis;

    }
    void setYesterday(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        lastAdUnitCallDay = calendar.get(Calendar.DAY_OF_YEAR);

    }






    private boolean isAllHighAdsNull(){
        return interstitialAd1 == null & interstitialAd2==null & interstitialAd3==null;
    }



    private void loadAds1(String adUnit) {
       /* String msg="";
        if(isAllHighAdsNull()) msg+=" null";
        else msg+=" not null";
        if(ad1IsLoading) msg+=" loading,";
        else msg+=" not loading,";*/




        if ( interstitialAd1 == null & !ad1IsLoading ) {
          //  Toast.makeText(ctx,msg,Toast.LENGTH_LONG).show();




            ad1IsLoading=true;



            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(ctx, adUnit, adRequest, new InterstitialAdLoadCallback() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    ad1IsLoading=false;
                    interstitialAd1 = null;


                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    ad1IsLoading=false;
                    AdUnit1LastCalledTime= System.currentTimeMillis();
                    interstitialAd1 = interstitialAd;
                    interstitialAd1.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd1 = null;


                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            interstitialAd1 = null;


                        }
                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            updatelastAdShownTime();
                        }
                    });
                }
            });


        }
    }



    private void loadAds2(String adUnit) {
        if ( interstitialAd2 == null & !ad2IsLoading ) {

            ad2IsLoading=true;
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(ctx, adUnit, adRequest, new InterstitialAdLoadCallback() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    ad2IsLoading=false;
                    interstitialAd2 = null;


                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    ad2IsLoading=false;
                    AdUnit2LastCalledTime= System.currentTimeMillis();
                    interstitialAd2 = interstitialAd;
                    interstitialAd2.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd2 = null;

                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            interstitialAd2 = null;


                        }
                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            updatelastAdShownTime();
                        }
                    });
                }
            });


        }
    }
    private void loadAds3(String adUnit) {
        if ( interstitialAd3 == null & !ad3IsLoading ) {
            ad3IsLoading=true;

            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(ctx, adUnit, adRequest, new InterstitialAdLoadCallback() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    ad3IsLoading=false;
                    interstitialAd3 = null;


                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    ad3IsLoading=false;
                    AdUnit3LastCalledTime= System.currentTimeMillis();
                    interstitialAd3 = interstitialAd;
                    interstitialAd3.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd3 = null;

                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            interstitialAd3 = null;


                        }
                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            updatelastAdShownTime();
                        }
                    });
                }
            });


        }
    }

    private void loadAds4(String adUnit) {
        if ( interstitialAd4 == null & !ad4IsLoading ) {
            ad4IsLoading=true;


            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(ctx, adUnit, adRequest, new InterstitialAdLoadCallback() {

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    ad4IsLoading=false;
                    interstitialAd4 = null;


                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    ad4IsLoading=false;
                    AdUnit4LastCalledTime= System.currentTimeMillis();
                    interstitialAd4 = interstitialAd;
                    interstitialAd4.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd4 = null;

                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            interstitialAd4 = null;


                        }
                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            updatelastAdShownTime();
                        }
                    });
                }
            });


        }
    }

}