package scutbci.lyl.sipcall;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class NetStim extends Dialog implements View.OnClickListener, SocketHandler, Runnable {

    private Context context;
    private int mPort = 5000;
    private String mServer = "192.168.0.100";
    private int mEvent = 1;
    private boolean mIsConnected;
    private UDPClient mSocket = null;
    private int mMaxEvents = 1000;
    private LinkedList<Integer> mEventQueue = null;

    public NetStim(Context context) {
        super(context);
        this.context = context;
    }

    public NetStim(Context context, int theme) {
        super(context, theme);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_netstim);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());

        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        lp.width = (int) (metrics.widthPixels * 0.6);  // 宽度设置为屏幕的百分比
        lp.height = (int) (metrics.heightPixels * 0.5); // 高度设置为屏幕的百分比
        dialogWindow.setAttributes(lp);

        ((EditText)findViewById(R.id.editServer)).setText(mServer);
        ((EditText)findViewById(R.id.editPort)).setText(String.valueOf(mPort));
        ((EditText)findViewById(R.id.editEvent)).setText(String.valueOf(mEvent));

        Button btnConnect = ((Button)findViewById(R.id.btnConnect));
        btnConnect.setFocusable(true);
        btnConnect.setFocusableInTouchMode(true);
        btnConnect.requestFocus();
        btnConnect.setOnClickListener(this);
        ((Button)findViewById(R.id.btnHide)).setOnClickListener(this);
        ((Button)findViewById(R.id.btnSend)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnect:
                onConnect();
                break;
            case R.id.btnSend:
                onSend();
                break;
            case R.id.btnHide:
                if (isShowing()) {
                    hide();
                }
                break;
        }
    }

    public void enableControls(boolean bConnect)
    {
        ((EditText)findViewById(R.id.editServer)).setEnabled(!bConnect);
        ((EditText)findViewById(R.id.editPort)).setEnabled(!bConnect);
        ((Button)findViewById(R.id.btnConnect)).setText(bConnect?"Disconnect":"Connect");
    }

    public void onConnect()
    {
        mServer = ((EditText)findViewById(R.id.editServer)).getText().toString();
        mPort = Integer.valueOf(((EditText)findViewById(R.id.editPort)).getText().toString());

        if (mSocket == null) {
            mSocket = new UDPClient(this);
            if (!mSocket.connect(mServer, mPort)) {
                Toast toast = Toast.makeText(getContext(),
                        "Failed to connect to " + mServer + ":" + mPort, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }

            new Thread(this).start();
            if (mEventQueue != null) {
                mEventQueue.retainAll(null);
                mEventQueue = null;
            }
            mEventQueue = new LinkedList<Integer>();
        } else {
            if (mSocket != null) {
                mSocket.disconnect();
                mSocket = null;
            }
            System.out.println("Disconnected from NetStim");
        }
        enableControls(isConnected());
    }

    public boolean isConnected()
    {
        return (mSocket != null) && mSocket.isConnected();
    }

    public void onSend()
    {
        mEvent = Integer.valueOf(((EditText)findViewById(R.id.editEvent)).getText().toString());
        outputEvent(mEvent);
    }

    public void outputEvent(int event)
    {
        // send(event);
        setEvent(event);
    }

    public void send(int event)
    {
        if (isConnected()) {
            byte[] data = new byte[4];
            ByteBuffer bufData = ByteBuffer.wrap(data);
            bufData.putInt(event);
            mSocket.send(bufData.array());
        } else {
            Toast toast = Toast.makeText(getContext(),
                    "NetStim not connected", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    @Override
    public void run() {
        int event = -1;
        while(isConnected()) {
            event = getEvent(true);
            if (event > 0) {
                send(event);
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setEvent(int event)
    {
        synchronized (this) {
            mEventQueue.offer(event);
            if (mEventQueue.size() > mMaxEvents)
            {
                getEvent(true);
            }
        }
    }

    private int getEvent(boolean bRemove)
    {
        synchronized (this) {
            int event = -1;
            if (mEventQueue.size() > 0) {
                if (bRemove) {
                    event = mEventQueue.poll();
                } else {
                    event = mEventQueue.peek();
                }
            }
            return event;
        }
    }

    @Override
    public void onReceive(IOBuffer buf) {

    }

    @Override
    public void onDisconnect() {
        enableControls(isConnected());
    }
}
