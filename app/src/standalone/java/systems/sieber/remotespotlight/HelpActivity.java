package systems.sieber.remotespotlight;

import android.annotation.SuppressLint;
import android.app.Dialog;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HelpActivity extends BaseHelpActivity {

    HelpActivity me;
    FeatureCheck mFc;

    private final static String UNLOCK_CODE_SHOP_URL = "https://georg-sieber.de/?page=app-remotepointer";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = this;

        // init manual unlock
        mButtonBuyKeyboard.setEnabled(true);
        mButtonBuyKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.remotespotlight.keyboard", "keyboard");
            }
        });
        mButtonBuyScanner.setEnabled(true);
        mButtonBuyScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUnlockInputBox("systems.sieber.remotespotlight.scanner", "scanner");
            }
        });
        loadPurchases();
    }

    private void loadPurchases() {
        // load in-app purchases
        mFc = new FeatureCheck(this);
        mFc.setFeatureCheckReadyListener(new FeatureCheck.featureCheckReadyListener() {
            @Override
            public void featureCheckReady(boolean fetchSuccess) {
                if(mFc.unlockedKeyboard) unlockPurchase("keyboard");
                if(mFc.unlockedScanner) unlockPurchase("scanner");
            }
        });
        mFc.init();
    }

    @SuppressWarnings("SameParameterValue")
    private void openUnlockInputBox(final String requestFeature, final String sku) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_inputbox);
        ((EditText) ad.findViewById(R.id.editTextInputBox)).setHint(R.string.unlock_code);
        ad.findViewById(R.id.buttonBuyCode).setVisibility(View.VISIBLE);
        ad.findViewById(R.id.buttonBuyCode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBrowser(UNLOCK_CODE_SHOP_URL);
            }
        });
        ad.findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String code = ((EditText) ad.findViewById(R.id.editTextInputBox)).getText().toString().trim();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String response = checkCode(requestFeature, code);
                            if(response == null) throw new Exception();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //JSONObject licenseInfo = new JSONObject(response);
                                    mFc.unlockPurchase(sku);
                                    loadPurchases();
                                }
                            });
                        } catch(Exception e) {
                            if(me == null || me.isFinishing() || me.isDestroyed()) return;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog(getString(R.string.activation_failed), e.getMessage()==null?"":e.getMessage(), "fail", false);
                                }
                            });
                        }
                    }
                }).start();
            }
        });
        if(ad.getWindow() != null)
            ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        ad.show();
    }

    String checkCode(String feature, String code) throws Exception {
        try {
            URL urlGetRequest = new URL(getResources().getString(R.string.unlock_api));
            HttpURLConnection conn = (HttpURLConnection) urlGetRequest.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Unlock-Feature", feature);
            conn.setRequestProperty("X-Unlock-Code", code);
            int statusCode = conn.getResponseCode();
            conn.disconnect();
            if(statusCode == 999) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuffer response = new StringBuffer();
                String inputLine;
                while((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                throw new Exception(getString(R.string.invalid_code) + " ("+statusCode+")");
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new Exception(getString(R.string.check_internet_conn));
        }
    }

}
