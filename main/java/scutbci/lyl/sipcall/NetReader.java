package scutbci.lyl.sipcall;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.LinkedList;

public class NetReader extends Dialog implements View.OnClickListener, MessageHandler, EpochHandler, Runnable {

    public class  AmpBasicInfo
    {
        int dwSize;            // Size of structure, used for version control
        int nEegChan;        // Number of EEG channels
        int nEvtChan;        // Number of event channels
        int nBlockPnts;        // Samples in block
        int nRate;            // Sampling rate (in Hz)
        int nDataSize;        // 2 for "short", 4 for "int" type of data
        float fResolution;    // Resolution for LSB

        public AmpBasicInfo() {
            this.dwSize = 0;
            this.nEegChan = 0;
            this.nEvtChan = 0;
            this.nBlockPnts = 0;
            this.nRate = 0;
            this.nDataSize = 0;
            this.fResolution = 0;
        }

        public AmpBasicInfo(byte[] buf) {
            ByteBuffer bbuf = ByteBuffer.wrap(buf);
            bbuf.order(ByteOrder.LITTLE_ENDIAN);
            this.dwSize = bbuf.getInt();
            this.nEegChan = bbuf.getInt();
            this.nEvtChan = bbuf.getInt();
            this.nBlockPnts = bbuf.getInt();
            this.nRate = bbuf.getInt();
            this.nDataSize = bbuf.getInt();
            this.fResolution = bbuf.getFloat();
        }

        public boolean validate()
        {
            return (nBlockPnts > 0 && nRate > 0 && nEegChan > 0  && nDataSize > 0);
        }
    }

    private Context context;
    private int mPort = 4000;
    private String mServer = "192.168.0.100";
    private int mScale = 200;
    private int mDuration = 2;
    private boolean mIsRunning = false;
    private boolean mIsConnected = false;
    private AmpTransport mAmpTransport = null;
    private int mAllChan = 0;
    private int mBlockPoints = 0;
    private int mTotalPoints = 0;
    private int[] mRawData = null;
    private AmpBasicInfo mBasicInfo = null;
    private EEGGraph mEEGGraph = null;
    private int mEpochLen = 150;
    private EpochManager mEpochManager = new EpochManager(this);
    private int mMaxEvents = 100;
    private LinkedList<Integer> mEventQueue = new LinkedList<Integer>();

    public NetReader(Context context)
    {
        super(context);
        this.context = context;
    }

    public NetReader(Context context, int theme) {
        super(context, theme);
        this.context = context;
    }

    @Override
    public void show() {
        super.show();
        if (mEEGGraph != null) mEEGGraph.startUpdate();
    }

    @Override
    public void hide() {
        super.hide();
        if (mEEGGraph != null) mEEGGraph.stopUpdate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_netreader);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());

        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        lp.width = (int) (metrics.widthPixels * 0.95);  // 宽度设置为屏幕的百分比
        lp.height = (int) (metrics.heightPixels * 0.9); // 高度设置为屏幕的百分比
        dialogWindow.setAttributes(lp);

        ((EditText)findViewById(R.id.editServer)).setText(mServer);
        ((EditText)findViewById(R.id.editPort)).setText(String.valueOf(mPort));
        ((TextView)findViewById(R.id.textDuration)).setText(String.valueOf(mDuration));
        ((TextView)findViewById(R.id.textScale)).setText(String.valueOf(mScale));

        Button btnConnect = ((Button)findViewById(R.id.btnConnect));
        btnConnect.setFocusable(true);
        btnConnect.setFocusableInTouchMode(true);
        btnConnect.requestFocus();
        btnConnect.setOnClickListener(this);
        ((Button)findViewById(R.id.btnHide)).setOnClickListener(this);

        LinearLayout layout = (LinearLayout) findViewById(R.id.netreader);
        mEEGGraph = new EEGGraph(context);
        layout.addView(mEEGGraph);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnect:
                onConnect();
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
        ((TextView)findViewById(R.id.textDuration)).setEnabled(!bConnect);
        ((Button)findViewById(R.id.btnConnect)).setText(bConnect?"Disconnect":"Connect");
    }

    public void onConnect()
    {
        mServer = ((EditText)findViewById(R.id.editServer)).getText().toString();
        mPort = Integer.valueOf(((EditText)findViewById(R.id.editPort)).getText().toString());

        if (mAmpTransport == null) {
            mAmpTransport = new AmpTransport(mServer, mPort, mMessageHandler);
            if (!mAmpTransport.start()) {
                Toast toast = Toast.makeText(getContext(),
                        "Failed to connect to "+mServer+":"+mPort, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                mAmpTransport = null;
                return;
            }
            showStatus("Server: "+mServer+":"+mPort);
        }
        else {
            stopAcquisition();
            if (mAmpTransport != null) {
                mAmpTransport.stop();
                mAmpTransport = null;
            }
            showStatus("Client stopped");
        }
        enableControls(isConnected());
    }

    public boolean isConnected()
    {
        mIsConnected = (mAmpTransport != null);
        return mIsConnected;
    }

    public void setEpochLen(int epochlen)
    {
        mEpochLen = epochlen;
    }

    public Epoch getEpoch()
    {
        return mEpochManager.getEpoch(true);
    }

    public void clearEpochs()
    {
        mEpochManager.clearEpochs();
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

    private void insertEvent(byte[] data, boolean b32bit, int numchan, int event)
    {
        ByteBuffer bufData = ByteBuffer.wrap(data);
        bufData.order(ByteOrder.LITTLE_ENDIAN);

        if (event > 0) {
            if (b32bit) {
                bufData.putInt(numchan*4, event);
            }
            else {
                bufData.putShort(numchan*2, (new Integer(event)).shortValue());
            }
        }
    }

    private void showStatus(final String strStatus)
    {
        ((TextView) findViewById(R.id.textStatus)).setText(strStatus);
    }

    private boolean startAcquisition()
    {
        if (mAmpTransport == null) return false;
        if (mIsRunning) return false;

        if (mBasicInfo == null) {
            System.out.println("Basic channel info unavailable. Look at CNetReaderDlg::AcquisitionStart() procedure.");
            return false;
        }
        mAllChan = mBasicInfo.nEegChan + mBasicInfo.nEvtChan;
        mBlockPoints = mBasicInfo.nBlockPnts;
        mTotalPoints = mBasicInfo.nRate*mDuration;

        if (!mBasicInfo.validate() || mTotalPoints <= 0) {
            System.out.println("Serious error with parameters.Look at CNetReaderDlg::AcquisitionStart() procedure.");
            return false;
        }
        mEpochManager.setChannelInfo(mBasicInfo.nEegChan, mBasicInfo.nEvtChan, mEpochLen);

        mRawData = new int[mTotalPoints*mAllChan];
        mEEGGraph.init(mAllChan, mTotalPoints, mScale);
        mEEGGraph.setDataSource(mRawData);
        mEEGGraph.startUpdate();

        (new Thread(this)).start();
        mAmpTransport.sendCommand(CTRLCODE.ClientControlCode, CTRLREQ.RequestStartData);
        mIsConnected = true;

        return true;
    }

    private boolean stopAcquisition()
    {
        if (mAmpTransport == null) return false;

        mAmpTransport.sendCommand(CTRLCODE.ClientControlCode, CTRLREQ.RequestStopData);
        mIsConnected = false;

        mIsRunning = false;
        new Thread(this).interrupt();

        mEpochManager.clearEpochs();
        mRawData = null;
        mEEGGraph.stopUpdate();

        return true;
    }

    @Override
    public void run() {
        int sample = 0;
        boolean b32bit = (mBasicInfo.nDataSize == (Integer.SIZE/8));
        int blocksize = mAllChan*mBlockPoints*mBasicInfo.nDataSize;
        byte[] data = new byte[blocksize];

        mIsRunning = true;
        while (mIsRunning) {
            if (!mAmpTransport.getData(data, blocksize)) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // insertEvent(data, b32bit, mBasicInfo.nEegChan, getEvent(true)); // for soft marking
                mEpochManager.insertData(data, mBasicInfo.nBlockPnts, b32bit);

                ByteBuffer bufData = ByteBuffer.wrap(data);
                bufData.order(ByteOrder.LITTLE_ENDIAN);
                if (mRawData != null) {
                    for (int i=0; i<mBlockPoints; i++)
                    {
                        for (int j=0; j<mAllChan; j++)
                        {
                            if (b32bit) {
                                mRawData[(i+sample)*mAllChan+j] = bufData.getInt();
                            } else {
                                mRawData[(i+sample)*mAllChan+j] = bufData.getShort();
                            }
                        }
                    }
                    sample = (sample+mBlockPoints)%mTotalPoints;
                }
            }
        }
        System.out.println("WorkThread in NetReader ended");
    }

    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            Bundle bundle = message.getData();
            AmpMessage msg = (AmpMessage)bundle.getSerializable("MSG");
            int msgId = message.what;
            switch (msgId) {
                case 1:
                    onCtrlMsg(msg);
                    break;
                case 2:
                    onDataMsg(msg);
                    break;
            }
        }
    };

    @Override
    public void onCtrlMsg(AmpMessage msg) {
        int code = msg.getCode();
        int request = msg.getRequest();
        if (code == CTRLCODE.GeneralControlCode) {
            switch (request)
            {
                case CTRLREQ.ClosingUp:
                case CTRLREQ.ServerDisconnected:
                    mEpochManager.clearEpochs();
                    mAmpTransport = null;
                    showStatus("Server disconnected");
                    break;
            }
        }
        else if (code == CTRLCODE.ServerControlCode) {
            String strStatus = "";
            switch (request)
            {
                case CTRLREQ.StartAcquisition:
                    if (startAcquisition()) {
                        strStatus = "Start Acquisition";
                    }
                    break;
                case CTRLREQ.StopAcquisition:
                    if (stopAcquisition()) {
                        strStatus = "Stop Acquisition";
                    }
                    break;
                case CTRLREQ.StartImpedance:
                    strStatus = "Start Impedance";
                    break;
                case CTRLREQ.DCCorrection:
                    strStatus = "DC Correction";
                    break;
                case CTRLREQ.ChangeSetup:
                    strStatus = "Setup was changed";
                    break;
            }
            showStatus(strStatus);
        }
        enableControls(isConnected());
    }

    @Override
    public void onDataMsg(AmpMessage msg) {
        int code = msg.getCode();
        int request = msg.getRequest();
        if (code == DATACODE.DataType_InfoBlock) {
            if (request == DATAREQ.InfoType_BasicInfo) {
                if (msg.hasData()) {
                    mBasicInfo = new AmpBasicInfo(msg.getData());
                    DecimalFormat df = new DecimalFormat("0.000");
                    showStatus("EEG:" + mBasicInfo.nEegChan + "+" + mBasicInfo.nEvtChan
                            + ";Pnts:" + mBasicInfo.nBlockPnts + ";" + mBasicInfo.nRate
                            + "Hz;Bits:" + mBasicInfo.nDataSize * 8 + ";Res:" + df.format(mBasicInfo.fResolution) + "uV/LSB");
                }
            }
        }
    }

    @Override
    public void onDataReady(Epoch epoch) {

    }
}
