package systems.sieber.remotespotlight;

import android.content.res.Resources;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.InputStream;

public class TextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.apache_license);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            ((TextView) findViewById(R.id.textViewText)).setText(new String(b));
        } catch (Exception e) {
            e.printStackTrace();
            ((TextView) findViewById(R.id.textViewText)).setText("???");
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}
