package systems.sieber.remotespotlight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.zxing.Result;

import java.util.Timer;
import java.util.TimerTask;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ControlActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    ControlActivity me;
    TcpClient mTcpClient;
    boolean sendValues = false;
    String authCode;
    FeatureCheck fc;

    private final int REQUEST_HELP = 1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        me = this;

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // do feature check
        fc = new FeatureCheck(this);
        fc.init();

        // set up InputView
        final View et = findViewById(R.id.editTextControlKeyboardImmediately);
        et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_UP) {
                    return false;
                }
                char unicodeChar = (char) event.getUnicodeChar();
                if(keyCode == KeyEvent.KEYCODE_ENTER) {
                    // handle ENTER
                    Log.e("KEYEVENT", "ENTER");
                    sendReturn();
                    return true;
                } else if(keyCode == KeyEvent.KEYCODE_DEL) {
                    // handle DEL
                    Log.e("KEYEVENT", "DEL");
                    sendBackspace();
                    return true;
                } else if(unicodeChar != 0) {
                    // handle normal chars
                    Log.e("KEYEVENT", "CHAR:" + unicodeChar);
                    if(fc == null || !fc.unlockedKeyboard) {
                        dialogInApp(getResources().getString(R.string.feature_locked_keyboard), getResources().getString(R.string.feature_locked_text));
                        return true;
                    }
                    sendMessage(unicodeChar + "");
                    return true;
                }
                // handle special chars (non-ASCII)
                Log.e("KEYEVENT", "CHARS:"+event.getCharacters());
                if(event.getCharacters() == null) return true;
                if(fc == null || !fc.unlockedKeyboard) {
                    dialogInApp(getResources().getString(R.string.feature_locked_keyboard), getResources().getString(R.string.feature_locked_text));
                    return true;
                }
                sendMessage(event.getCharacters() + "");
                return true;
            }
        });

        CheckBox cb = findViewById(R.id.checkBoxControlKeyboardSendImmediately);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    findViewById(R.id.viewKeyboardInput).setVisibility(View.VISIBLE);
                    findViewById(R.id.editTextControlKeyboardText).setVisibility(View.GONE);
                    findViewById(R.id.buttonSendText).setVisibility(View.INVISIBLE);
                    findViewById(R.id.viewKeyboardInput).requestFocus();
                    showKeyboard();
                } else {
                    findViewById(R.id.viewKeyboardInput).setVisibility(View.GONE);
                    findViewById(R.id.editTextControlKeyboardText).setVisibility(View.VISIBLE);
                    findViewById(R.id.buttonSendText).setVisibility(View.VISIBLE);
                    findViewById(R.id.editTextControlKeyboardText).requestFocus();
                }
            }
        });

        (findViewById(R.id.buttonSpotlight)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        if(mTcpClient != null) mTcpClient.sendMessage("START");
                        sendValues = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if(mTcpClient != null) mTcpClient.sendMessage("STOP");
                        sendValues = false;
                        break;
                }
                return false;
            }
        });
        (findViewById(R.id.buttonMouseLeft)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        if(mTcpClient != null) mTcpClient.sendMessage("MDOWN");
                        break;
                    case MotionEvent.ACTION_UP:
                        if(mTcpClient != null) mTcpClient.sendMessage("MUP");
                        break;
                }
                return false;
            }
        });
        (findViewById(R.id.buttonMouseRight)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        if(mTcpClient != null) mTcpClient.sendMessage("MRIGHT");
                        break;
                }
                return false;
            }
        });
        (findViewById(R.id.buttonTouchpad)).setOnTouchListener(new View.OnTouchListener() {
            private int maxPixelMovementForMouseClick = 5;
            private int _xDelta;
            private int _yDelta;
            private int _xDown;
            private int _yDown;
            long startTime;
            boolean sendMouse = false;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final int X = (int) event.getRawX();
                final int Y = (int) event.getRawY();
                switch(event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        _xDelta = X;
                        _yDelta = Y;
                        _xDown = X;
                        _yDown = Y;
                        sendMouse = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        sendMouse = false;
                        long difference = System.currentTimeMillis() - startTime;
                        if(difference < 400
                                && Math.abs(Math.abs(_xDown) - Math.abs(X)) < maxPixelMovementForMouseClick
                                && Math.abs(Math.abs(_yDown) - Math.abs(Y)) < maxPixelMovementForMouseClick) {
                            if(mTcpClient != null) mTcpClient.sendMessage("MLEFT");
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(sendMouse && mTcpClient != null && X - _xDelta != 0 && Y - _yDelta != 0)
                            mTcpClient.sendMessage("M"+"|"+Integer.toString(X - _xDelta)+"|"+Integer.toString(Y - _yDelta));
                        _xDelta = X;
                        _yDelta = Y;
                        break;
                }
                return true;
            }
        });

        //dv = (DemoView) findViewById(R.id.view_demo);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if(rotationVectorSensor == null) {
            Log.e("sensors", "Sensor not available.");
        }
        SensorEventListener sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                //dv.update(-sensorEvent.values[2] * 10, -sensorEvent.values[0] * 10);
                if(mTcpClient != null && sendValues) {
                    if(Math.abs(sensorEvent.values[2]) > 0.01f && Math.abs(sensorEvent.values[0]) > 0.01f)
                        mTcpClient.sendMessage("S"+"|"+Float.toString(-sensorEvent.values[2])+"|"+Float.toString(-sensorEvent.values[0]));
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        sensorManager.registerListener(
                sensorListener,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
        );

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // establish connection to server
        Intent intent = getIntent();
        authCode = intent.getStringExtra("authCode");
        new ConnectTask(
                intent.getStringExtra("address"),
                intent.getIntExtra("port",4444)
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // send periodic ping packets
        TimerTask taskCheckEvent = new TimerTask() {
            @Override
            public void run() {
                // server will disconnect if no message received within 5 seconds
                if(mTcpClient != null) mTcpClient.sendMessage("PING");
            }
        };
        new Timer(false).schedule(taskCheckEvent, 0, 2000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_show_mouse:
                findViewById(R.id.linearLayoutControlDefaults).setVisibility(View.VISIBLE);
                findViewById(R.id.linearLayoutControlKeyboard).setVisibility(View.GONE);
                findViewById(R.id.constraintLayoutControlScanner).setVisibility(View.GONE);
                if(mScannerView != null) mScannerView.stopCamera();
                hideKeyboard( (EditText)findViewById(R.id.editTextControlKeyboardText) );
                break;
            case R.id.action_open_keyboard:
                findViewById(R.id.linearLayoutControlDefaults).setVisibility(View.GONE);
                findViewById(R.id.linearLayoutControlKeyboard).setVisibility(View.VISIBLE);
                findViewById(R.id.constraintLayoutControlScanner).setVisibility(View.GONE);
                if(mScannerView != null) mScannerView.stopCamera();
                showKeyboard();
                break;
            case R.id.action_start_scanner:
                setupCamera();
                findViewById(R.id.linearLayoutControlDefaults).setVisibility(View.GONE);
                findViewById(R.id.linearLayoutControlKeyboard).setVisibility(View.GONE);
                findViewById(R.id.constraintLayoutControlScanner).setVisibility(View.VISIBLE);
                hideKeyboard( (EditText)findViewById(R.id.editTextControlKeyboardText) );
                break;
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void dialogInApp(String title, String text) {
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setTitle(title);
        ad.setMessage(text);
        ad.setIcon(getResources().getDrawable(R.drawable.ic_warning_orange_24dp));
        ad.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setNeutralButton(getResources().getString(R.string.more), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivityForResult(new Intent(me, HelpActivity.class), REQUEST_HELP);
            }
        });
        ad.show();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
        findViewById(R.id.editTextControlKeyboardImmediately).requestFocus();
    }
    private void hideKeyboard(EditText et) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm != null) {
            imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if((keyCode == KeyEvent.KEYCODE_BACK))
            finish();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish() {
        if(mTcpClient != null) mTcpClient.stopClient();
        super.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(findViewById(R.id.constraintLayoutControlScanner).getVisibility() == View.VISIBLE
                && mScannerView != null) {
            mScannerView.startCamera();
        }
        if(findViewById(R.id.linearLayoutControlKeyboard).getVisibility() == View.VISIBLE
                && ((CheckBox) findViewById(R.id.checkBoxControlKeyboardSendImmediately)).isChecked()) {
            showKeyboard();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard( (EditText)findViewById(R.id.editTextControlKeyboardText) );
        if(mScannerView != null) mScannerView.stopCamera();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_HELP) {
            Log.i("FEATURECHECK", "init new feature check");
            fc = new FeatureCheck(this);
            fc.init();
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        vibrate();
        if(fc != null && fc.unlockedScanner) {
            String code = rawResult.getText();
            if(mTcpClient != null) {
                mTcpClient.sendMessage("TEXT|"+code);
                if(((CheckBox) findViewById(R.id.checkBoxControlScannerReturn)).isChecked())
                    mTcpClient.sendMessage("RETURN");
            }
            dialogScanned(code);
        } else {
            dialogInApp(getResources().getString(R.string.feature_locked_scanner), getResources().getString(R.string.feature_locked_text));
        }
    }

    private void dialogScanned(String text) {
        new AlertDialog.Builder(this)
                .setMessage( text )
                .setPositiveButton(getResources().getString(R.string.next_scan), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mScannerView.resumeCameraPreview(me);
                    }})
                //.setNegativeButton(getResources().getString(R.string.abort), null)
                .show();
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(v != null) {
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            //deprecated in API 26
            if(v != null) {
                v.vibrate(100);
            }
        }
    }

    public void sendMessageFromTextBox(View v) {
        if(fc != null && fc.unlockedKeyboard) {
            EditText et = findViewById(R.id.editTextControlKeyboardText);
            for(String line : et.getText().toString().split("\n")) {
                sendMessage(line);
                try {
                    Thread.sleep(5);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                sendReturn();
                try {
                    Thread.sleep(5);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
            et.setText("");
        } else {
            dialogInApp(getResources().getString(R.string.feature_locked_keyboard), getResources().getString(R.string.feature_locked_text));
        }
    }
    public void sendMessage(String text) {
        if(!text.equals("")) {
            if(mTcpClient != null) mTcpClient.sendMessage("TEXT|"+text);
        }
    }
    public void sendReturn() {
        if(mTcpClient != null) mTcpClient.sendMessage("RETURN");
    }
    public void sendBackspace() {
        if(mTcpClient != null) mTcpClient.sendMessage("BACKSPACE");
    }
    public void sendEscape(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("ESCAPE");
    }
    public void sendUp(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("UP");
    }
    public void sendDown(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("DOWN");
    }
    public void sendLeft(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("LEFT");
    }
    public void sendRight(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("RIGHT");
    }
    public void sendPrev(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("PREV");
    }
    public void sendNext(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("NEXT");
    }
    public void sendF1(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F1");
    }
    public void sendF2(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F2");
    }
    public void sendF3(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F3");
    }
    public void sendF4(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F4");
    }
    public void sendF5(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F5");
    }
    public void sendF6(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F6");
    }
    public void sendF7(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F7");
    }
    public void sendF8(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F8");
    }
    public void sendF9(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F9");
    }
    public void sendF10(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F10");
    }
    public void sendF11(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F11");
    }
    public void sendF12(View v) {
        if(mTcpClient != null) mTcpClient.sendMessage("F12");
    }

    private final static int CAMERA_PERMISSION = 1;
    private ZXingScannerView mScannerView;
    private void setupCamera() {
        // init scanner
        mScannerView = findViewById(R.id.scannerView);
        mScannerView.setFlash(false);
        mScannerView.setAutoFocus(true);
        mScannerView.setAspectTolerance(0.5f);
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();

        // check camera permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }
    }

    public enum messageType {
        normalExit,
        authFailed,
        connectionFailed,
        connectionClosed
    }
    private void finishWithMessage(messageType m) {
        Intent returnIntent = getIntent();
        returnIntent.putExtra("result",m);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    public void showSoftKeyboard(View v) {
        showKeyboard();
    }


    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        private String address;
        private int port;

        ConnectTask(String _address, int _port) {
            address = _address;
            port = _port;
        }

        @Override
        protected TcpClient doInBackground(String... message) {

            mTcpClient = new TcpClient(address, port,
                new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    if(message.equals("HELLO!")) {
                        Log.d("Sending authcode", "--> "+authCode);
                        mTcpClient.sendMessage(authCode);
                    }
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            },
            new TcpClient.OnConnectionClosed() {
                @Override
                public void connectionClosed(boolean authFailed) {
                    if(authFailed) finishWithMessage(messageType.authFailed);
                    else finishWithMessage(messageType.connectionClosed);
                }
            },
            new TcpClient.OnConnectionFailed() {
                @Override
                public void connectionFailed() {
                    finishWithMessage(messageType.connectionFailed);
                }
            });
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.d("Server", values[0]);
        }

    }
}
