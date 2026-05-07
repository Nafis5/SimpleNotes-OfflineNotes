package com.notes.keepnotes;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * Centralized helper for persisting premium entitlement state locally so we can
 * route instantly while asynchronously confirming against the backend.
 */
public final class PremiumPrefs {
    private static final String PREFS_NAME = "premium_prefs";
    private static final String KEY_IS_PREMIUM = "is_premium";
    private static final String KEY_LAST_PURCHASE_ID = "last_purchase_id";

    private PremiumPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void savePremiumState(Context context, boolean isPremium, @Nullable String purchaseId) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(KEY_IS_PREMIUM, isPremium);
        if (purchaseId != null && !purchaseId.isEmpty()) {
            editor.putString(KEY_LAST_PURCHASE_ID, purchaseId);
        }
        editor.apply();
        CheckPremiumStatus.isPremium = isPremium;
    }

    public static void updatePremiumFlag(Context context, boolean isPremium) {
        prefs(context).edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
        CheckPremiumStatus.isPremium = isPremium;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
        CheckPremiumStatus.isPremium = false;
    }

    public static boolean getIsPremium(Context context) {
        return prefs(context).getBoolean(KEY_IS_PREMIUM, false);
    }

    @Nullable
    public static String getLastPurchaseId(Context context) {
        return prefs(context).getString(KEY_LAST_PURCHASE_ID, null);
    }
}
