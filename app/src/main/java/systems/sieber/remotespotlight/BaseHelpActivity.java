package systems.sieber.remotespotlight;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class BaseHelpActivity extends AppCompatActivity {

    SharedPreferences mSettings;

    Button mButtonBuyKeyboard;
    Button mButtonBuyScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // find views
        mButtonBuyKeyboard = findViewById(R.id.buttonBuyKeyboard);
        mButtonBuyScanner = findViewById(R.id.buttonBuyScanner);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSettings = getSharedPreferences(ConnectActivity.PREFS_NAME, 0);
        try {
            ((TextView) findViewById(R.id.textViewVersion)).setText(
                    String.format(getResources().getString(R.string.version), getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
            );
        } catch(PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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

    void unlockPurchase(String sku) {
        SharedPreferences.Editor editor = mSettings.edit();
        switch(sku) {
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

    void openBrowser(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch(SecurityException | ActivityNotFoundException ignored) {
            dialog(getString(R.string.no_web_browser_found), url, "warn", false);
        }
    }

    void dialog(String title, String text, String icon, final boolean finishIntent) {
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
                if(finishIntent) finish();
            }
        });
        ad.show();
    }

}
