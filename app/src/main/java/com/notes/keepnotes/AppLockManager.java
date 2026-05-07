package com.notes.keepnotes;


// AppLockManager.java
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppLockManager {
    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final SharedPreferences encryptedPrefs;

    public AppLockManager(Context context) {
        this.context = context;
        // Initialize EncryptedSharedPreferences (from Android Security Library)
        encryptedPrefs = getEncryptedSharedPreferences(context);
    }

    private SharedPreferences getEncryptedSharedPreferences(Context context) {
        // Use EncryptedSharedPreferences (requires Security library dependency)
        String prefsFile = context.getPackageName() + "_applock";
        return context.getSharedPreferences(prefsFile, Context.MODE_PRIVATE);
    }

    // Check if device has a secure lock screen
    public boolean isDeviceSecure() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.KeyguardManager keyguardManager =
                    (android.app.KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return keyguardManager.isDeviceSecure();
        }
        return false;
    }

    // Check if app lock is enabled
    public boolean isAppLockEnabled() {
        return encryptedPrefs.getBoolean("app_lock_enabled", false);
    }

    // Toggle app lock state (with validation)
    public void setAppLockEnabled(boolean enabled) {
        if (enabled && !isDeviceSecure()) {
            Toast.makeText(context,
                    "Set a device lock screen (PIN/pattern/password) first!",
                    Toast.LENGTH_LONG).show();
            return;
        }
        encryptedPrefs.edit().putBoolean("app_lock_enabled", enabled).apply();
    }

    // Show authentication prompt
    public void showAuthPrompt(FragmentActivity activity, AuthCallback callback) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Notes")
                .setSubtitle("Authenticate to access your notes")
                .setDeviceCredentialAllowed(true) // Allow PIN/pattern/password
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        callback.onError(errString.toString());
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }
}

