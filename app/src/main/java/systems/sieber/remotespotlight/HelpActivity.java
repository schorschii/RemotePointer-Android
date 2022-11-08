package systems.sieber.remotespotlight;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HelpActivity extends AppCompatActivity {

    private BillingClient mBillingClient;
    SharedPreferences mSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSettings = getSharedPreferences(ConnectActivity.PREFS_NAME, 0);
        try {
            ((TextView)findViewById(R.id.textViewVersion)).setText(
                    String.format(getResources().getString(R.string.version), getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
            );
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // do feature check
        final FeatureCheck fc = new FeatureCheck(this);
        fc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                if(fc.unlockedKeyboard) unlockPurchase("keyboard", null);
                if(fc.unlockedScanner) unlockPurchase("scanner", null);
            }
        });
        fc.init();

        // init billing client
        mBillingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
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
                                        unlockPurchase(sku, purchase);
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

    public void onClickEmailLink(View v) {
        final Intent emailIntent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.parse("mailto:"
                + getResources().getString(R.string.email_address)
                + "?subject=" + "Feedback RemotePointer"
                + "&body=" + "");
        emailIntent.setData(data);
        startActivity(Intent.createChooser(emailIntent, "Feedback"));
    }

    public void onClickWebLink(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.website_url)));
        startActivity(browserIntent);
    }

    private void unlockPurchase(String sku, Purchase purchase) {
        SharedPreferences.Editor editor = mSettings.edit();
        switch (sku) {
            case "keyboard":
                ((ImageView) findViewById(R.id.imageViewBuyKeyboard)).setImageResource(R.drawable.tick_green);
                editor.putBoolean("purchased-keyboard", true);
                editor.apply();
                break;
            case "scanner":
                ((ImageView) findViewById(R.id.imageViewBuyScanner)).setImageResource(R.drawable.tick_green);
                editor.putBoolean("purchased-scanner", true);
                editor.apply();
                break;
        }
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
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> productDetailsList) {
                int responseCode = billingResult.getResponseCode();
                if(responseCode == BillingClient.BillingResponseCode.OK) {
                    for(final ProductDetails skuDetails : productDetailsList) {
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
    public void doBuyKeyboard(View v) {
        doBuy(mSkuDetailsKeyboard, null);
    }
    public void doBuyScanner(View v) {
        doBuy(mSkuDetailsScanner, null);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showOnGithub(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.repo_url)));
        startActivity(browserIntent);
    }

    public void showApacheLicense(View v) {
        Intent i = new Intent(this, TextActivity.class);
        startActivity(i);
    }

    private void dialog(String title, String text, String icon, final boolean finishIntent) {
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setCancelable(!finishIntent);
        if(title != null && !title.equals("")) ad.setTitle(title);
        if(icon != null && icon.equals("ok")) {
            if(text != null && (!text.equals(""))) ad.setMessage(text);
            ad.setIcon(getResources().getDrawable(R.drawable.tick_green));
        } else if(icon != null && icon.equals("fail")) {
            if(text != null && (!text.equals(""))) ad.setMessage(text);
            ad.setIcon(getResources().getDrawable(R.drawable.fail));
        } else if(icon != null && icon.equals("warn")) {
            if(text != null && (!text.equals(""))) ad.setMessage(text);
            ad.setIcon(getResources().getDrawable(R.drawable.ic_warning_orange_24dp));
        } else {
            ad.setMessage(text);
        }
        ad.setButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (finishIntent) finish();
            }
        });
        ad.show();
    }

}
