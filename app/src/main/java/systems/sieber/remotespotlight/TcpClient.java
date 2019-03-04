package systems.sieber.remotespotlight;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

class TcpClient {

    private static final String TAG = TcpClient.class.getSimpleName();
    private String SERVER_IP;
    private int SERVER_PORT;

    private String mServerMessage;

    private OnMessageReceived mMessageListener;
    private OnConnectionClosed mConnectionClosedListener;
    private OnConnectionFailed mConnectionFailedListener;

    private boolean mRun = false;
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;


    TcpClient(String _address, int _port, OnMessageReceived receivedListener, OnConnectionClosed closedListener, OnConnectionFailed failedListener) {
        SERVER_IP = _address;
        SERVER_PORT = _port;
        mMessageListener = receivedListener;
        mConnectionClosedListener = closedListener;
        mConnectionFailedListener = failedListener;
    }

    void sendMessage(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null) {
                    Log.d(TAG, "Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    void stopClient() {
        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    void run() {

        mRun = true;
        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

            Socket socket;
            try {
                socket = new Socket(serverAddr, SERVER_PORT);
            } catch (Exception e) {
                if(mConnectionFailedListener != null)
                    mConnectionFailedListener.connectionFailed();
                return;
            }

            try {
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                boolean authFailed = false;
                while (mRun) {
                    mServerMessage = mBufferIn.readLine();

                    if (mServerMessage != null) {
                        if(mServerMessage.equals("AUTHFAILED")) {
                            authFailed = true;
                        }
                        if(mMessageListener != null) {
                            mMessageListener.messageReceived(mServerMessage);
                        }
                    }

                    if (mServerMessage == null) {
                        mRun = false;
                        if (mConnectionClosedListener != null)
                            mConnectionClosedListener.connectionClosed(authFailed);
                    }
                }

                Log.d("RESPONSE FROM SERVER", "Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {
                Log.e("TCP", "Error", e);
            } finally {
                socket.close();
            }

        } catch (Exception e) {
            Log.e("TCP", "Error", e);
        }

    }

    public interface OnMessageReceived {
        void messageReceived(String message);
    }

    public interface OnConnectionClosed {
        void connectionClosed(boolean authFailed);
    }

    public interface OnConnectionFailed {
        void connectionFailed();
    }

}
