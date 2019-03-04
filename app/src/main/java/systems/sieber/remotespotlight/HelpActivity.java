package systems.sieber.remotespotlight;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AppCompatActivity {

    private BillingClient mBillingClient;
    SharedPreferences mSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        fc.useCache = false;
        fc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                if(fc.unlockedKeyboard) unlockPurchase("keyboard");
                if(fc.unlockedScanner) unlockPurchase("scanner");
            }
        });
        fc.init();

        // init billing client
        mBillingClient = BillingClient.newBuilder(this).setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
                if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
                    for (Purchase purchase : purchases) {
                        unlockPurchase(purchase.getSku());
                    }
                } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
                    dialog(
                            getResources().getString(R.string.purchase_canceled),
                            getResources().getString(R.string.purchase_canceled_description),
                            "warn",
                            false
                    );
                } else if (responseCode == BillingClient.BillingResponse.ITEM_ALREADY_OWNED) {
                    try {
                        dialog(
                                getResources().getString(R.string.purchase_already_done),
                                getResources().getString(R.string.purchase_already_done_description),
                                "ok",
                                false
                        );
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                } else {
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
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if(billingResponseCode == BillingClient.BillingResponse.OK) {
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
                + getResources().getString(R.string.emailaddress)
                + "?subject=" + "Feedback RemotePointer"
                + "&body=" + "");
        emailIntent.setData(data);
        startActivity(Intent.createChooser(emailIntent, "Feedback"));
    }

    public void onClickWebLink(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.websiteurl)));
        startActivity(browserIntent);
    }

    private void unlockPurchase(String sku) {
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
        ArrayList<String> skuList = new ArrayList<>();
        skuList.add("keyboard");
        skuList.add("scanner");
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        mBillingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSkuDetailsResponse(int responseCode, List skuDetailsList) {
                if (responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                    for (Object skuDetails : skuDetailsList) {
                        String sku = ((SkuDetails)skuDetails).getSku();
                        String price = ((SkuDetails)skuDetails).getPrice();
                        switch (sku) {
                            case "keyboard":
                                ((Button) findViewById(R.id.buttonBuyKeyboard)).setText(
                                        price+"\n"+getResources().getString(R.string.buy_now)
                                );
                                (findViewById(R.id.buttonBuyKeyboard)).setEnabled(true);
                                break;
                            case "scanner":
                                ((Button) findViewById(R.id.buttonBuyScanner)).setText(
                                        price+"\n"+getResources().getString(R.string.buy_now)
                                );
                                (findViewById(R.id.buttonBuyScanner)).setEnabled(true);
                                break;
                        }
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

    private void doBuy(String sku) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSku(sku)
                .setType(BillingClient.SkuType.INAPP)
                .build();
        int responseCode = mBillingClient.launchBillingFlow(this, flowParams);
    }
    public void doBuyKeyboard(View v) {
        doBuy("keyboard");
    }
    public void doBuyScanner(View v) {
        doBuy("scanner");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
            ad.setIcon(getResources().getDrawable(R.drawable.ic_warning_gray_24dp));
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
