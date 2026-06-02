package com.notes.keepnotes;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import com.samsung.android.sdk.iap.lib.helper.IapHelper;
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener;
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo;
import com.samsung.android.sdk.iap.lib.vo.ProductVo;

import org.json.JSONObject;

import java.util.ArrayList;

public class SubscriptionActivity2 extends AppCompatActivity {

    private static final String TAG = "SubscriptionActivity2";
    private static final String VERIFY_BASE_URL = "https://chatbot-apis-123.uc.r.appspot.com/verify_receipt?purchaseID=";
    private static final int MAX_VERIFICATION_ATTEMPTS = 2; // initial try + one retry after delay
    private static final long VERIFICATION_RETRY_DELAY_MS = 10000L; // 10 second wait for Samsung to propagate
    private SamsungIAPManager iapManager;
    private LinearLayout yearlyCard;
    private LinearLayout monthlyCard;
    private TextView yearlyPriceText;
    private TextView monthlyPriceText;
    private Button continueButton;
    private String selectedPlanId = null;
    private ProgressBar verificationSpinner;
    private TextView restoreLink;
    private boolean isRestoringPurchase = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 64, 48, 64);
        layout.setGravity(Gravity.TOP);
        scrollView.setBackgroundColor(Color.parseColor("#0E1116"));

        FrameLayout header = new FrameLayout(this);
        header.setPadding(0, 0, 0, 16);

        ImageButton closeButton = new ImageButton(this);
        closeButton.setBackground(null);
        closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeButton.setColorFilter(Color.parseColor("#C9D1D9"));
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL
        );
        closeButton.setLayoutParams(closeParams);
        closeButton.setOnClickListener(v -> navigateBackToMain());
        header.addView(closeButton);

        TextView badge = new TextView(this);
        badge.setText("PREMIUM");
        badge.setAllCaps(true);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(12f);
        badge.setPadding(32, 12, 32, 12);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(createRoundedBackground("#1F6FEB", 100, true));
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        badge.setLayoutParams(badgeParams);
        header.addView(badge);
        layout.addView(header);

        TextView title = new TextView(this);
        title.setText("Unlock Note Pro");
        title.setTextSize(30f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 32, 0, 8);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Get unlimited access to all features.");
        subtitle.setTextColor(Color.parseColor("#C9D1D9"));
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, 32);
        layout.addView(subtitle);

        LinearLayout benefitsCard = new LinearLayout(this);
        benefitsCard.setOrientation(LinearLayout.VERTICAL);
        benefitsCard.setPadding(32, 32, 32, 32);
        benefitsCard.setBackground(createRoundedBackground("#161B22", 32, false));
        benefitsCard.setGravity(Gravity.START);

        TextView benefitsTitle = new TextView(this);
        benefitsTitle.setText("Included in Premium");
        benefitsTitle.setTextColor(Color.WHITE);
        benefitsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        benefitsTitle.setTextSize(16f);
        benefitsCard.addView(benefitsTitle);

      //  addBenefitRow(benefitsCard, "Data Backup & Restore", "Keep your notes safe in the cloud.");
        addBenefitRow(benefitsCard, "Export Notes", "Export notes as pdf, markdown, html");
        addBenefitRow(benefitsCard, "App Locking", "Protect your notes with biometric security.");
        addBenefitRow(benefitsCard, "Voice Typing", "Type notes with voice commands.");

        layout.addView(benefitsCard);

        LinearLayout plansContainer = new LinearLayout(this);
        plansContainer.setOrientation(LinearLayout.VERTICAL);
        plansContainer.setPadding(0, 32, 0, 16);
        layout.addView(plansContainer);

        yearlyPriceText = createPriceText("$30");
        monthlyPriceText = createPriceText("$5");

        yearlyCard = createPlanCard("Yearly", "Best value • Save 50%", yearlyPriceText, "2.5 USD/month");
        yearlyCard.setOnClickListener(v -> selectPlan(SamsungIAPManager.ITEM_ID_YEARLY));
        plansContainer.addView(yearlyCard);

        monthlyCard = createPlanCard("Monthly", "Flexibility, cancel anytime", monthlyPriceText, null);
        monthlyCard.setOnClickListener(v -> selectPlan(SamsungIAPManager.ITEM_ID_MONTHLY));
        plansContainer.addView(monthlyCard);

        continueButton = new Button(this);
        continueButton.setText("Continue");
        continueButton.setAllCaps(false);
        continueButton.setTextSize(18f);
        continueButton.setTextColor(Color.WHITE);
        continueButton.setPadding(24, 24, 24, 24);
        continueButton.setBackground(createRoundedBackground("#1F6FEB", 48, true));
        continueButton.setEnabled(false);
        continueButton.setAlpha(0.6f);
        continueButton.setOnClickListener(v -> {
            if (selectedPlanId == null) {
                Toast.makeText(this, "Select a plan to continue", Toast.LENGTH_SHORT).show();
            } else {
                initiatePurchase(selectedPlanId);
            }
        });
        layout.addView(continueButton);

        verificationSpinner = new ProgressBar(this);
        verificationSpinner.setIndeterminate(true);
        verificationSpinner.setVisibility(View.GONE);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.gravity = Gravity.CENTER_HORIZONTAL;
        verificationSpinner.setLayoutParams(spinnerParams);
        layout.addView(verificationSpinner);

        restoreLink = new TextView(this);
        restoreLink.setText("Restore purchases");
        restoreLink.setTextColor(Color.parseColor("#58A6FF"));
        restoreLink.setGravity(Gravity.CENTER_HORIZONTAL);
        restoreLink.setPadding(0, 24, 0, 0);
        LinearLayout.LayoutParams restoreParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        restoreParams.gravity = Gravity.CENTER_HORIZONTAL;
        restoreParams.topMargin = 16;
        restoreLink.setLayoutParams(restoreParams);
        restoreLink.setOnClickListener(v -> triggerRestoreFromPaywall());
        layout.addView(restoreLink);

        scrollView.addView(layout);
        setContentView(scrollView);

        // Initialize IAP Manager
        iapManager = SamsungIAPManager.getInstance(this);

        fetchProductPrices();
    }

    private void initiatePurchase(String itemId) {
        Toast.makeText(this, "Starting purchase...", Toast.LENGTH_SHORT).show();

        iapManager.startPayment(itemId, (errorVo, purchaseVo) -> {
            if (errorVo.getErrorCode() == IapHelper.IAP_ERROR_NONE) {
                // Payment Successful
                if (purchaseVo != null) {
                    handleSuccessfulPurchase(purchaseVo);
                }
            } else {
                // Payment Failed or Cancelled
                String err = errorVo.getErrorString();
                Log.e(TAG, "Payment failed: " + err);
                Toast.makeText(SubscriptionActivity2.this,
                    "Payment Failed: " + err, Toast.LENGTH_LONG).show();
                showStatus("Payment Failed: " + err, true);
            }
        });
    }

    private void fetchProductPrices() {
        iapManager.getProductDetails((errorVo, productList) -> {
            if (errorVo != null && errorVo.getErrorCode() != IapHelper.IAP_ERROR_NONE) {
                runOnUiThread(() -> showStatus("Pricing error: " + errorVo.getErrorString(), true));
                return;
            }

            if (productList == null) {
                runOnUiThread(() -> showStatus("Pricing unavailable", true));
                return;
            }

            runOnUiThread(() -> updatePriceViews(productList));
        });
    }

    private void handleSuccessfulPurchase(PurchaseVo purchaseVo) {
        String purchaseId = purchaseVo.getPurchaseId();

        Log.d(TAG, "Purchase Successful. ID: " + purchaseId);
        showStatus("Verifying subscription...", false);
        setVerificationLoading(true);
        Toast.makeText(this, "Purchase Successful! Please wait...", Toast.LENGTH_SHORT).show();

        runVerificationWithRetry(purchaseId, 1);
    }

    private void runVerificationWithRetry(String purchaseId, int attempt) {
        verifyWithServer(purchaseId, (isValid, responseBody) -> {
            if (isValid) {
                acknowledgePurchase(purchaseId);
            } else if (attempt < MAX_VERIFICATION_ATTEMPTS) {
                int nextAttempt = attempt + 1;
                showStatus("Still confirming your subscription... please hold (" + nextAttempt + " of " + MAX_VERIFICATION_ATTEMPTS + ")", false);
                setVerificationLoading(true);
                new Handler(Looper.getMainLooper()).postDelayed(
                    () -> runVerificationWithRetry(purchaseId, nextAttempt),
                    VERIFICATION_RETRY_DELAY_MS
                );
            } else {
                Toast.makeText(SubscriptionActivity2.this, "Server verification failed", Toast.LENGTH_LONG).show();
                showStatus("Server verification failed. Please try again.", true);
                setVerificationLoading(false);
            }
        });
    }

    private void triggerRestoreFromPaywall() {
        if (isRestoringPurchase) {
            return;
        }
        isRestoringPurchase = true;
        setVerificationLoading(true);
        showStatus("Checking for previous purchases...", false);
        setRestoreLinkEnabled(false);

        iapManager.restorePurchases((isPremium, restoredPurchaseId) -> runOnUiThread(() -> {
            isRestoringPurchase = false;
            setVerificationLoading(false);
            setRestoreLinkEnabled(true);

            if (isPremium) {
                PremiumPrefs.savePremiumState(SubscriptionActivity2.this, true, restoredPurchaseId);
                Toast.makeText(SubscriptionActivity2.this, "Subscription restored!", Toast.LENGTH_LONG).show();
                showStatus("Premium restored. Redirecting...", false);
                navigateBackToMain();
            } else {
                showStatus("No active subscription found.", true);
            }
        }));
    }

    private void acknowledgePurchase(String purchaseId) {
        iapManager.acknowledgePurchase(purchaseId, (errorVo, acknowledgedList) -> {
            if (errorVo != null && errorVo.getErrorCode() == IapHelper.IAP_ERROR_NONE) {
                Log.d(TAG, "Purchase Acknowledged Successfully");
                Toast.makeText(SubscriptionActivity2.this, "Subscription Active!", Toast.LENGTH_LONG).show();
                showStatus("Subscription Active!", false);
                PremiumPrefs.savePremiumState(SubscriptionActivity2.this, true, purchaseId);
                setVerificationLoading(false);
                navigateBackToMain();
            } else {
                String err = (errorVo != null) ? errorVo.getErrorString() : "unknown";
                Log.e(TAG, "Acknowledgement failed: " + err);
                Toast.makeText(SubscriptionActivity2.this, "Acknowledge failed: " + err, Toast.LENGTH_LONG).show();
                showStatus("Acknowledge failed: " + err, true);
                setVerificationLoading(false);
            }
        });
    }

    private void verifyWithServer(String purchaseId, VerificationCallback callback) {
        String serverUrl = VERIFY_BASE_URL + purchaseId;
        Log.d(TAG, "Verifying with server: " + serverUrl);

        new Thread(() -> {
            java.net.HttpURLConnection conn = null;
            try {
                java.net.URL url = new java.net.URL(serverUrl);
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Server Response Code: " + responseCode);

                if (responseCode == 200) {
                    java.io.InputStream inputStream = conn.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();

                    String responseBody = responseBuilder.toString();
                    JSONObject obj = new JSONObject(responseBody);
                    boolean isValid = obj.optBoolean("is_valid", false);
                    runOnUiThread(() -> callback.onResult(isValid, responseBody));
                } else {
                    String errorBody = readStream(conn.getErrorStream());
                    final String finalBody = (errorBody == null || errorBody.isEmpty())
                            ? ("HTTP " + responseCode)
                            : errorBody;
                    runOnUiThread(() -> {
                        callback.onResult(false, finalBody);
                        showStatus("Server verification HTTP " + responseCode, true);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Verification Error", e);
                String message = "Verification error: " + e.getMessage();
                runOnUiThread(() -> {
                    callback.onResult(false, message);
                    showStatus(message, true);
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void setVerificationLoading(boolean isLoading) {
        if (verificationSpinner != null) {
            verificationSpinner.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void setRestoreLinkEnabled(boolean enabled) {
        if (restoreLink != null) {
            restoreLink.setEnabled(enabled);
            restoreLink.setAlpha(enabled ? 1f : 0.5f);
        }
    }

    private String readStream(java.io.InputStream stream) {
        if (stream == null) {
            return null;
        }
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();
            return responseBuilder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void showStatus(String message, boolean isError) {
        Log.d(TAG, (isError ? "[error] " : "") + message);
    }

    private void updatePriceViews(ArrayList<ProductVo> products) {
        for (ProductVo product : products) {
            String productId = null;
            String priceText = null;
            try {
                productId = product.getItemId();
            } catch (Exception ignored) {}

            if (productId == null || productId.isEmpty()) {
                // fallback to JSON to extract ID if SDK getter unavailable
                productId = extractFromJson(product.getJsonString(), new String[]{"itemId", "productId", "ITEM_ID"});
            }

            if (productId == null) {
                Log.w(TAG, "Product missing itemId, skipping: " + product.getJsonString());
                continue;
            }

            try {
                priceText = product.getItemPriceString();
            } catch (Exception ignored) {}

            if (priceText == null || priceText.isEmpty()) {
                priceText = extractFromJson(product.getJsonString(), new String[]{"itemPriceString", "priceString", "formattedPrice", "ITEM_PRICE_STRING"});
            }

            if (SamsungIAPManager.ITEM_ID_YEARLY.equals(productId)) {
                yearlyPriceText.setText(priceText != null ? priceText : "Yearly price");
            } else if (SamsungIAPManager.ITEM_ID_MONTHLY.equals(productId)) {
                monthlyPriceText.setText(priceText != null ? priceText : "Monthly price");
            }
        }
    }

    private String extractFromJson(String jsonString, String[] keys) {
        if (jsonString == null || jsonString.isEmpty()) return null;
        try {
            JSONObject json = new JSONObject(jsonString);
            for (String key : keys) {
                String value = json.optString(key, null);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "JSON parse failed", e);
        }
        return null;
    }

    private void selectPlan(String planId) {
        selectedPlanId = planId;
        stylePlanCard(yearlyCard, SamsungIAPManager.ITEM_ID_YEARLY.equals(planId));
        stylePlanCard(monthlyCard, SamsungIAPManager.ITEM_ID_MONTHLY.equals(planId));
        updateContinueButtonState();
    }

    private void stylePlanCard(LinearLayout card, boolean selected) {
        if (card == null) return;
        GradientDrawable drawable = createRoundedBackground(selected ? "#1F6FEB" : "#161B22", 40, true);
        drawable.setStroke(selected ? 4 : 2, Color.parseColor(selected ? "#8AB4FF" : "#30363D"));
        card.setBackground(drawable);
        card.setAlpha(selected ? 1f : 0.9f);
    }

    private void updateContinueButtonState() {
        boolean enabled = selectedPlanId != null;
        continueButton.setEnabled(enabled);
        continueButton.setAlpha(enabled ? 1f : 0.6f);
    }

    private GradientDrawable createRoundedBackground(String colorHex, int radius, boolean solid) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radius);
        drawable.setColor(Color.parseColor(colorHex));
        if (!solid) {
            drawable.setStroke(2, Color.parseColor("#30363D"));
        }
        return drawable;
    }

    private void addBenefitRow(LinearLayout parent, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 20, 0, 20);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextSize(15f);
        row.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(Color.parseColor("#C9D1D9"));
        subtitleView.setTextSize(14f);
        row.addView(subtitleView);

        parent.addView(row);
    }

    private TextView createPriceText(String defaultText) {
        TextView view = new TextView(this);
        view.setText(defaultText);
        view.setTextColor(Color.WHITE);
        view.setTextSize(20f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private LinearLayout createPlanCard(String title, String caption, TextView priceView, @Nullable String cornerText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(48, 48, 48, 48);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
        params.setMargins(0, 16, 0, 16);
        card.setLayoutParams(params);
        card.setBackground(createRoundedBackground("#161B22", 40, true));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(Color.WHITE);
        label.setTextSize(18f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(label);

        card.addView(priceView);

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setPadding(0, 8, 0, 0);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView desc = new TextView(this);
        desc.setText(caption);
        desc.setTextColor(Color.parseColor("#8B949E"));
        desc.setTextSize(14f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        desc.setLayoutParams(descParams);
        bottomRow.addView(desc);

        if (cornerText != null) {
            TextView cornerLabel = new TextView(this);
            cornerLabel.setText(cornerText);
            cornerLabel.setTextColor(Color.parseColor("#8B949E"));
            cornerLabel.setTextSize(14f);
            cornerLabel.setGravity(Gravity.END);
            bottomRow.addView(cornerLabel);
        }

        card.addView(bottomRow);

        return card;
    }

    private void navigateBackToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private interface VerificationCallback {
        void onResult(boolean isValid, String responseBody);
    }
}
