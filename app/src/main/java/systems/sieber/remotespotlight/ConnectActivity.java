package systems.sieber.remotespotlight;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnectActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ConnectActivity me = this;
    ListenForBroadcastTask broadcastListener;
    ListView listViewServer;
    List<ControlComputer> availComputers = new ArrayList<>();

    final static String PREFS_NAME           = "remotepointer";
    private static final int broadcastPort   = 4445;
    private static final int controlPort     = 4444;
    private static final int REQUEST_CONTROL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conn);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // init welcome text with app version
        TextView textViewConnectInfo = findViewById(R.id.textViewConnectInfo);
        textViewConnectInfo.setText(String.format(getResources().getString(R.string.add_computer), getResources().getString(R.string.desktopapp_ver)));

        // init Drawer
        DrawerLayout drawer = findViewById(R.id.drawerLayoutMain);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.navigationViewMain);
        navigationView.setNavigationItemSelectedListener(this);

        // init action button
        /*FloatingActionButton fabHelp = findViewById(R.id.fab);
        fabHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { }
        });*/

        // init server list view
        listViewServer = findViewById(R.id.listViewServer);
        listViewServer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ControlComputer computerObject = (ControlComputer) listViewServer.getItemAtPosition(position);
                dialogAuthCode(computerObject.address, controlPort);
            }
        });

        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(5000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                removeStaleComputers();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcastListener = new ConnectActivity.ListenForBroadcastTask(this, broadcastPort);
        broadcastListener.execute("");
    }

    @Override
    public void onPause() {
        super.onPause();
        broadcastListener.cancel(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawerLayoutMain);
        drawer.closeDrawer(GravityCompat.START);
        switch(item.getItemId()) {
            case R.id.nav_manual_conn:
                dialogIP();
                break;
            case R.id.nav_information:
                Intent helpIntent = new Intent(me, HelpActivity.class);
                startActivity(helpIntent);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CONTROL) {
            if (resultCode == Activity.RESULT_OK) {
                ControlActivity.messageType result = (ControlActivity.messageType) data.getSerializableExtra("result");
                Log.e("messageType", result.toString());
                connMessage(result);
            }
        }
    }

    private void connMessage(ControlActivity.messageType m) {
        if(m != ControlActivity.messageType.normalExit) {
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setCancelable(false);
            ad.setTitle(getResources().getString(R.string.connfailed_title));
            if(m == ControlActivity.messageType.authFailed) ad.setMessage(getResources().getString(R.string.authfailed_text));
            else if(m == ControlActivity.messageType.connectionFailed) ad.setMessage(getResources().getString(R.string.connfailed_text));
            else if(m == ControlActivity.messageType.connectionClosed) ad.setMessage(getResources().getString(R.string.connclosed_text));
            ad.setIcon(getResources().getDrawable(R.drawable.fail));
            ad.setButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            ad.show();
        }
    }

    private void removeStaleComputers() {
        List<ControlComputer> newComputers = new ArrayList<>();
        for(ControlComputer cc : availComputers) {
            if((new Date().getTime() - cc.recognized.getTime())/1000 < 5)
                newComputers.add(cc);
        }
        availComputers = newComputers;
        bindToListView(availComputers);
    }

    private void bindToListView(List<ControlComputer> cc) {
        if(cc == null) return;
        listViewServer.setAdapter(new ControlComputerAdapter(this, cc));
        if(cc.size() > 0) {
            findViewById(R.id.listViewServer).setVisibility(View.VISIBLE);
            findViewById(R.id.textViewConnectInfo).setVisibility(View.INVISIBLE);
        } else {
            findViewById(R.id.listViewServer).setVisibility(View.INVISIBLE);
            findViewById(R.id.textViewConnectInfo).setVisibility(View.VISIBLE);
        }
    }

    private void dialogAuthCode(final String address, final int port) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_authcode);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        ((EditText)ad.findViewById(R.id.editTextAuthCode)).setText(settings.getString("authCode",""));
        ((EditText)ad.findViewById(R.id.editTextAuthCode)).selectAll();

        ad.findViewById(R.id.buttonConnectionOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String authCode = ((EditText)(ad.findViewById(R.id.editTextAuthCode))).getText().toString();
                openControlActivity(
                        address, port, authCode
                );
                SharedPreferences settings_w = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings_w.edit();
                editor.putString("authCode", authCode);
                editor.apply();
            }
        });
        ad.findViewById(R.id.buttonConnectionCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        if(ad.getWindow() != null)
            ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        ad.show();
    }

    @SuppressLint("SetTextI18n")
    private void dialogIP() {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_connection);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        ((EditText)ad.findViewById(R.id.editTextAddress)).setText(settings.getString("address",""));
        ((EditText)ad.findViewById(R.id.editTextPort)).setText(Integer.toString(settings.getInt("port",controlPort)));
        ((EditText)ad.findViewById(R.id.editTextAuthCode)).setText(settings.getString("authCode",""));
        ((EditText)ad.findViewById(R.id.editTextAddress)).selectAll();

        ad.findViewById(R.id.buttonConnectionOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
                String address = ((EditText)(ad.findViewById(R.id.editTextAddress))).getText().toString();
                int port = Integer.parseInt(((EditText)(ad.findViewById(R.id.editTextPort))).getText().toString());
                String authCode = ((EditText)(ad.findViewById(R.id.editTextAuthCode))).getText().toString();
                openControlActivity(
                        address, port, authCode
                );
                SharedPreferences settings_w = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings_w.edit();
                editor.putString("address", address);
                editor.putInt("port", port);
                editor.putString("authCode", authCode);
                editor.apply();
            }
        });
        ad.findViewById(R.id.buttonConnectionCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });
        if(ad.getWindow() != null)
            ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        ad.show();
    }

    private void openControlActivity(String address, int port, String authCode) {
        Intent controlIntent = new Intent(me, ControlActivity.class);
        controlIntent.putExtra("address", address);
        controlIntent.putExtra("port", port);
        controlIntent.putExtra("authCode", authCode);
        startActivityForResult(controlIntent, REQUEST_CONTROL);
    }


    public static class ListenForBroadcastTask extends AsyncTask<String, String, TcpClient> {

        private WeakReference<ConnectActivity> activityReference;
        private int port;

        ListenForBroadcastTask(ConnectActivity context, int port) {
            this.activityReference = new WeakReference<>(context);
            this.port = port;
        }

        @Override
        protected TcpClient doInBackground(String... message) {

            // debug
            //publishProgress("10.0.1.1", "HELLOREMOTEPOINTER|Debug");

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            DatagramSocket socket = null;
            try {
                // Keep a socket open to listen to all the UDP trafic that is destined for this port
                socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
                socket.setBroadcast(true);

                while(!isCancelled()) {
                    // Receive a packet
                    byte[] recvBuf = new byte[15000];
                    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(packet);

                    // Packet received
                    String senderAddress = packet.getAddress().getHostAddress();
                    String data = new String(packet.getData()).trim();
                    //Log.i("UDP", "Packet received from: " + senderAddress);
                    if(data.startsWith("HELLOREMOTEPOINTER"))
                        publishProgress(senderAddress, data);
                }
            } catch (IOException ex) {
                Log.e("UDP", "Oops: " + ex.getMessage());
            }
            try {
                if(socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ex) {
                Log.e("UDP", "Oops: " + ex.getMessage());
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            ConnectActivity activity = activityReference.get();
            if (activity == null) return;

            String hostname = "???";
            try {
                hostname = values[1].split("\\|")[1];
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                boolean add = true;
                for(ControlComputer cc : activity.availComputers) {
                    if(cc.address.equals(values[0]) && cc.hostname.equals(hostname))
                        add = false;
                    cc.recognized = new Date();
                }
                if(add) {
                    activity.availComputers.add(new ControlComputer(hostname, values[0], new Date()));
                    activity.bindToListView(activity.availComputers);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
