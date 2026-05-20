package systems.sieber.remotespotlight;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.google.android.material.snackbar.Snackbar;

import android.view.View;
import android.widget.Button;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HelpActivity extends BaseHelpActivity {

    private BillingClient mBillingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init buy buttons
        mButtonBuyKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsKeyboard, null);
            }
        });
        mButtonBuyKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doBuy(mSkuDetailsScanner, null);
            }
        });

        // do feature check
        final FeatureCheck fc = new FeatureCheck(this);
        fc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(fc.unlockedKeyboard) unlockPurchase("keyboard");
                        if(fc.unlockedScanner) unlockPurchase("scanner");
                    }
                });
            }
        });
        fc.init();

        // init billing client
        mBillingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
                int responseCode = billingResult.getResponseCode();
                if(responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for(final Purchase purchase : purchases) {
                        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            for(final String sku : purchase.getProducts()) {
                                runOnUiThread(new Runnable(){
                                    @Override
                                    public void run() {
                                        unlockPurchase(sku);
                                    }
                                });
                            }
                            FeatureCheck.acknowledgePurchase(mBillingClient, purchase);
                        }
                    }
                } else if(responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    dialog(
                            getResources().getString(R.string.purchase_canceled),
                            getResources().getString(R.string.purchase_canceled_description),
                            "warn",
                            false
                    );
                } else if(responseCode != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                    try {
                        dialog(
                                getResources().getString(R.string.purchase_failed),
                                getResources().getString(R.string.check_internet_conn),
                                "fail",
                                false
                        );
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    querySkus();
                } else {
                    Snackbar.make(
                            findViewById(R.id.helpMainView),
                            getResources().getString(R.string.store_not_avail) + " - " +
                                    getResources().getString(R.string.could_not_fetch_prices),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                dialog(
                        getResources().getString(R.string.store_not_avail),
                        getResources().getString(R.string.check_internet_conn),
                        "warn",
                        true
                );
            }
        });
    }

    private void querySkus() {
        ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("keyboard").setProductType(BillingClient.ProductType.INAPP).build());
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("scanner").setProductType(BillingClient.ProductType.INAPP).build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();
        mBillingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                int responseCode = billingResult.getResponseCode();
                if(responseCode == BillingClient.BillingResponseCode.OK) {
                    for(final ProductDetails skuDetails : queryProductDetailsResult.getProductDetailsList()) {
                        final String sku = skuDetails.getProductId();
                        final String price = Objects.requireNonNull(skuDetails.getOneTimePurchaseOfferDetails()).getFormattedPrice();
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                setupPayButton(sku, price, skuDetails);
                            }
                        });
                    }
                } else {
                    dialog(
                            getResources().getString(R.string.store_not_avail),
                            getResources().getString(R.string.could_not_fetch_prices),
                            "warn",
                            false
                    );
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setupPayButton(String sku, String price, ProductDetails skuDetails) {
        switch(sku) {
            case "keyboard":
                mSkuDetailsKeyboard = skuDetails;
                ((Button) findViewById(R.id.buttonBuyKeyboard)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                (findViewById(R.id.buttonBuyKeyboard)).setEnabled(true);
                break;
            case "scanner":
                mSkuDetailsScanner = skuDetails;
                ((Button) findViewById(R.id.buttonBuyScanner)).setText(price+"\n"+getResources().getString(R.string.buy_now));
                (findViewById(R.id.buttonBuyScanner)).setEnabled(true);
                break;
        }
    }

    ProductDetails mSkuDetailsKeyboard;
    ProductDetails mSkuDetailsScanner;

    private BillingResult doBuy(ProductDetails sku, String offerToken) {
        if(sku == null) return null;
        BillingFlowParams.ProductDetailsParams.Builder builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(sku);
        if(offerToken != null) builder.setOfferToken(offerToken);
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(builder.build());
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();
        return mBillingClient.launchBillingFlow(this, flowParams);
    }

}
