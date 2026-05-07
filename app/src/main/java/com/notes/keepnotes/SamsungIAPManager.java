package com.notes.keepnotes;

import static com.samsung.android.sdk.iap.lib.constants.HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION;
import static com.samsung.android.sdk.iap.lib.constants.HelperDefine.OperationMode.OPERATION_MODE_TEST;

import android.content.Context;

import com.samsung.android.sdk.iap.lib.helper.IapHelper;
import com.samsung.android.sdk.iap.lib.listener.OnAcknowledgePurchasesListener;
import com.samsung.android.sdk.iap.lib.listener.OnGetOwnedListListener;
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener;
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener;
import com.samsung.android.sdk.iap.lib.vo.ErrorVo;
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo;

import androidx.annotation.Nullable;

/**
 * Singleton class to manage Samsung In-App Purchase logic.
 */
public class SamsungIAPManager {
    private static SamsungIAPManager instance;
    private IapHelper mIapHelper;

    // TODO: Replace with your actual Item IDs from Samsung Seller Portal
    public static final String ITEM_ID_MONTHLY = "sub_monthly";
    public static final String ITEM_ID_YEARLY = "sub_yearly_trial";

    // Operation Mode: Use IapHelper.IAP_MODE_TEST for development, IapHelper.IAP_MODE_PRODUCTION for release
    //private static final int OPERATION_MODE = IapHelper.IAP_MODE_TEST;

    private SamsungIAPManager(Context context) {
        Context appContext = context.getApplicationContext();
        // 1. Instantiate IapHelper
        mIapHelper = IapHelper.getInstance(appContext);
        // 2. Set Operation Mode
        mIapHelper.setOperationMode(OPERATION_MODE_PRODUCTION);
    }

    public static synchronized SamsungIAPManager getInstance(Context context) {
        if (instance == null) {
            instance = new SamsungIAPManager(context);
        }
        return instance;
    }

    /**
     * Fetch product details (Price, Name, etc.) from Samsung IAP Server.
     */
    public void getProductDetails(OnGetProductsDetailsListener listener) {
        String productIds = ITEM_ID_MONTHLY + "," + ITEM_ID_YEARLY;
        mIapHelper.getProductsDetails(productIds, listener);
    }

    /**
     * Start the payment process for a specific item.
     * @param itemId The ID of the item to purchase.
     * @param listener Callback for payment result.
     */
    public void startPayment(String itemId, OnPaymentListener listener) {
        // "passThroughParam" is deprecated but still part of the signature in older SDKs. 
        // Passing empty string as we use purchaseID for verification.
        mIapHelper.startPayment(itemId, "", listener);
    }

    /**
     * Acknowledges a subscription purchase. 
     * IMPORTANT: Subscriptions must be acknowledged or they may be refunded/cancelled.
     */
    public void acknowledgePurchase(String purchaseId, OnAcknowledgePurchasesListener listener) {
        mIapHelper.acknowledgePurchases(purchaseId, listener);
    }

    /**
     * Restore previously owned items/subscriptions and update premium flag.
     */
    public void restorePurchases(final RestoreCallback callback) {
        mIapHelper.getOwnedList(IapHelper.PRODUCT_TYPE_ALL, new OnGetOwnedListListener() {
            @Override
            public void onGetOwnedProducts(ErrorVo errorVo, java.util.ArrayList<OwnedProductVo> ownedList) {
                boolean isPremium = false;
                String restoredPurchaseId = null;

                if (errorVo != null && errorVo.getErrorCode() == IapHelper.IAP_ERROR_NONE && ownedList != null) {
                    for (OwnedProductVo item : ownedList) {
                        String itemId = item.getItemId();
                        if (ITEM_ID_MONTHLY.equals(itemId) || ITEM_ID_YEARLY.equals(itemId)) {
                            // Optional: inspect expiry with item.getExpireDate()
                            isPremium = true;
                            try {
                                restoredPurchaseId = item.getPurchaseId();
                            } catch (Exception ignored) {
                                restoredPurchaseId = null;
                            }
                            break;
                        }
                    }
                } else if (errorVo != null) {
                    android.util.Log.e("SamsungIAPManager", "Restore error: " + errorVo.getErrorString());
                }

                CheckPremiumStatus.isPremium = isPremium;

                if (callback != null) {
                    callback.onRestoreComplete(isPremium, restoredPurchaseId);
                }
            }
        });
    }

    public interface RestoreCallback {
        void onRestoreComplete(boolean isPremium, @Nullable String purchaseId);
    }
}
