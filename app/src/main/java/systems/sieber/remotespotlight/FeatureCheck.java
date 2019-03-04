package systems.sieber.remotespotlight;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.Purchase;
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
        mBillingClient = BillingClient.newBuilder(context).setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
            }
        }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if(billingResponseCode == BillingClient.BillingResponse.OK) {
                    queryPurchases();
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

    private void queryPurchases() {
        if(useCache) {
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
            if(purchasesResult.getResponseCode() == BillingClient.BillingResponse.OK) {
                for(Purchase p : purchasesResult.getPurchasesList()) {
                    unlockPurchase(p.getSku());
                }
                if(listener != null) listener.featureCheckReady(true);
            } else {
                if(listener != null) listener.featureCheckReady(false);
            }
            isReady = true;
        } else {
            mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
                @Override
                public void onPurchaseHistoryResponse(int responseCode, List<Purchase> purchasesList) {
                    if(responseCode == BillingClient.BillingResponse.OK) {
                        for(Purchase p : purchasesList) {
                            unlockPurchase(p.getSku());
                        }
                        if(listener != null) listener.featureCheckReady(true);
                    } else {
                        if(listener != null) listener.featureCheckReady(false);
                    }
                    isReady = true;
                }
            });
        }
    }

    boolean isReady = false;
    boolean unlockedKeyboard = false;
    boolean unlockedScanner = false;

    private void unlockPurchase(String sku) {
        switch (sku) {
            case "keyboard":
                unlockedKeyboard = true;
                break;
            case "scanner":
                unlockedScanner = true;
                break;
        }
    }
}
