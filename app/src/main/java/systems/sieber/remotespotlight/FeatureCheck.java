package systems.sieber.remotespotlight;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.List;

class FeatureCheck {
    private BillingClient mBillingClient;
    private FeatureCheck me;
    private Context context;
    boolean useCache = true;

    FeatureCheck(Context c) {
        me = this;
        context = c;
    }

    private featureCheckReadyListener listener = null;
    public interface featureCheckReadyListener {
        void featureCheckReady(boolean fetchSuccess);
    }
    void setFeatureCheckReadyListener(featureCheckReadyListener listener) {
        this.listener = listener;
    }

    void init() {
        // get settings (faster than google play - after purchase done, billing client needs minutes to realize the purchase)
        SharedPreferences settings = context.getSharedPreferences(ConnectActivity.PREFS_NAME, 0);
        unlockedKeyboard = settings.getBoolean("purchased-keyboard", false);
        unlockedScanner = settings.getBoolean("purchased-scanner", false);

        // init billing client - get purchases later for other devices
        mBillingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
            }
        }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if(useCache) {
                        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
                        if(purchasesResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for(Purchase p : purchasesResult.getPurchasesList()) {
                                unlockPurchase(p.getSku());
                                acknowledgePurchase(mBillingClient, p);
                            }
                            if(listener != null) listener.featureCheckReady(true);
                        } else {
                            if(listener != null) listener.featureCheckReady(false);
                        }
                        isReady = true;
                    } else {
                        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
                            @Override
                            public void onPurchaseHistoryResponse(@NonNull BillingResult billingResult, @Nullable List<PurchaseHistoryRecord> list) {
                                // not implemented anymore
                            }
                        });
                    }
                } else {
                    isReady = true;
                    if(listener != null) listener.featureCheckReady(false);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
            }
        });
    }

    static void acknowledgePurchase(BillingClient client, Purchase purchase) {
        if(!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) { }
            });
        }
    }

    boolean isReady = false;
    boolean unlockedKeyboard = false;
    boolean unlockedScanner = false;

    private void unlockPurchase(String sku) {
        switch(sku) {
            case "keyboard":
                unlockedKeyboard = true;
                break;
            case "scanner":
                unlockedScanner = true;
                break;
        }
    }
}
