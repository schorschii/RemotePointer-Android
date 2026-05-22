package systems.sieber.remotespotlight;

import android.content.Context;
import android.content.SharedPreferences;

public class BaseFeatureCheck {

    /*  It is not allowed to modify this file in order to bypass license checks.
        I made this app open source hoping people will learn something from this project.
        But keep in mind: open source means free as "free speech" but not as in "free beer".
        Please be so kind and support further development by purchasing the in-app purchases in one of the app stores.
        It's up to you how long this app will be maintained. Thanks for your support.
    */

    Context mContext;
    SharedPreferences mSettings;

    BaseFeatureCheck(Context c) {
        mContext = c;
    }

    featureCheckReadyListener listener = null;
    public interface featureCheckReadyListener {
        void featureCheckReady(boolean fetchSuccess);
    }
    void setFeatureCheckReadyListener(featureCheckReadyListener listener) {
        this.listener = listener;
    }

    void init() {
        // get settings (faster than google play - after purchase done, billing client needs minutes to realize the purchase)
        mSettings = mContext.getSharedPreferences(ConnectActivity.PREFS_NAME, 0);
        unlockedKeyboard = mSettings.getBoolean("purchased-keyboard", false);
        unlockedScanner = mSettings.getBoolean("purchased-scanner", false);
    }

    boolean isReady = false;

    boolean unlockedKeyboard = false;
    boolean unlockedScanner = false;

    void unlockPurchase(String sku) {
        SharedPreferences.Editor editor = mSettings.edit();
        switch(sku) {
            case "keyboard":
                unlockedKeyboard = true;
                editor.putBoolean("purchased-keyboard", true);
                break;
            case "scanner":
                unlockedScanner = true;
                editor.putBoolean("purchased-scanner", true);
                break;
        }
        editor.apply();
    }

}
