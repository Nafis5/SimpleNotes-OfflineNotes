package com.notes.keepnotes;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class OnboardingActivity extends AppCompatActivity {

    private static final String TAG = "OnboardingActivity";
    private static final String VERIFY_BASE_URL = "https://chatbot-apis-123.uc.r.appspot.com/verify_receipt?purchaseID=";

    private TextView statusTextView;
    private ProgressBar progressBar;
    private Button continueButton;
    private ImageView appIcon;
    private ObjectAnimator iconAnimator;
    private AppLockManager appLockManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        statusTextView = findViewById(R.id.restoreStatus);
        progressBar = findViewById(R.id.restoreProgress);
        continueButton = findViewById(R.id.continueButton);
        appIcon = findViewById(R.id.appIcon);
        appLockManager = new AppLockManager(this);

       // startIconAnimation();
        routeUsingCachedState();
        verifyPremiumSilently();
    }

    private void routeUsingCachedState() {
        boolean cachedPremium = PremiumPrefs.getIsPremium(this);
        CheckPremiumStatus.isPremium = cachedPremium;

        progressBar.setVisibility(View.GONE);
        continueButton.setEnabled(true);
        continueButton.setAlpha(1f);

        if (cachedPremium) {
            statusTextView.setText("Premium active. Unlocking your notes...");
            continueButton.setOnClickListener(v -> checkAppLockState());
            checkAppLockState();
        } else {
            statusTextView.setText("No subscription detected. Showing plans...");
            continueButton.setOnClickListener(v -> openSubscription());
            openSubscription();
        }
    }

    private void verifyPremiumSilently() {
        final String purchaseId = PremiumPrefs.getLastPurchaseId(this);
        if (purchaseId == null || purchaseId.isEmpty()) {
            Log.d(TAG, "No cached purchaseId; skipping backend verification.");
            return;
        }

        new Thread(() -> {
            Boolean serverResult = fetchPremiumFromServer(purchaseId);
            if (serverResult == null) {
                return;
            }
            runOnUiThread(() -> PremiumPrefs.updatePremiumFlag(OnboardingActivity.this, serverResult));
        }).start();
    }

    private void openMain() {
        stopIconAnimation();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void openSubscription() {
        stopIconAnimation();
        Intent intent = new Intent(this, SubscriptionActivity2.class);
        startActivity(intent);
        finish();
    }

    private void startIconAnimation() {
        if (appIcon == null) return;
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f);
        iconAnimator = ObjectAnimator.ofPropertyValuesHolder(appIcon, scaleX, scaleY);
        iconAnimator.setDuration(1400);
        iconAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        iconAnimator.start();
    }

    private void stopIconAnimation() {
        if (iconAnimator != null) {
            iconAnimator.cancel();
            iconAnimator = null;
        }
        if (appIcon != null) {
            appIcon.setScaleX(1f);
            appIcon.setScaleY(1f);
        }
    }
    private void checkAppLockState() {
        boolean isLockEnabled = appLockManager.isAppLockEnabled();


        if (isLockEnabled) {
            // Check if device still has a secure lock screen

            if (appLockManager.isDeviceSecure()) {

                showAuthenticationPrompt();
            } else {
                // Device lock was removed → disable app lock
                appLockManager.setAppLockEnabled(false);
                Toast.makeText(
                        this,
                        "App lock disabled! Device lock screen removed.",
                        Toast.LENGTH_LONG
                ).show();
                openMain();
            }
        } else {

            openMain();
        }
    }

    private void showAuthenticationPrompt() {
        appLockManager.showAuthPrompt(this, new AppLockManager.AuthCallback() {
            @Override
            public void onSuccess() {
                // Authentication successful → proceed to MainActivity
                openMain();
            }

            @Override
            public void onError(String error) {
                // Authentication failed or canceled → close app
                finishAffinity();
            }
        });
    }

    @Nullable
    private Boolean fetchPremiumFromServer(String purchaseId) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(VERIFY_BASE_URL + purchaseId);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                String body = readStream(conn.getInputStream());
                return evaluateSubscriptionEnd(body);
            } else {
                Log.w(TAG, "verifyPremiumSilently HTTP " + code);
            }
        } catch (Exception e) {
            Log.w(TAG, "verifyPremiumSilently failed", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    @Nullable
    private Boolean evaluateSubscriptionEnd(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONObject samsungData = root.optJSONObject("samsung_data");
            if (samsungData == null) {
                return null;
            }
            String endDate = samsungData.optString("subscriptionEndDate", null);
            if (endDate == null || endDate.isEmpty()) {
                return null;
            }
            Long expiryMillis = parseSubscriptionEndDate(endDate);
            if (expiryMillis == null) {
                return null;
            }
            return System.currentTimeMillis() < expiryMillis;
        } catch (Exception e) {
            Log.w(TAG, "evaluateSubscriptionEnd parse error", e);
            return null;
        }
    }

    @Nullable
    private Long parseSubscriptionEndDate(String dateString) {
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(dateString).getTime();
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private String readStream(InputStream stream) {
        if (stream == null) {
            return null;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
