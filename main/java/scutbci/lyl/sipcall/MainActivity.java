package scutbci.lyl.sipcall;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.sip.*;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles all calling based on BCI
 * Created by lyl.
 */

public class MainActivity extends Activity implements Runnable {

    private enum FSMSTATE {
        STATE_START,
        STATE_PREPARE,
        STATE_ROUNDINIT,
        STATE_ROUND,
        STATE_REST
    }

    private class EVENT {
        private final static int DATA_BEGIN = 1;
        private final static int DATA_END = 40;
        private final static int START = 81;
    }

    // Call Function with Dial Pad
    private EditText mSipAddress;
    private FlashButton[] mDialPad;
    private int			mMargin;
    private int			mNumTarget;  // 可输出的目标类别数量，比如36个字符无论行列闪还是单独闪都是36
    private String[] items;

    // The Sip Call Info
    private SipSetApplication sipSet;
    private SipAudioCall call;

    private FSMSTATE		mRunState;
    private boolean		mUserStop;
    private int			mNumStimuli; // 对目标进行编码后的刺激数，比如36个字符行列闪是12，单独闪是36
    private int			mMaxTrial;
    private	int				mStimIndex;
    private int			mRoundIndex;
    private	int				mCurrentTrial;
    private boolean		mWaitForNextTrial;

    private Timer mTimerFSM;
    private Thread         mThreadData;
    private int			mTimeStart2Prepare;
    private int			mTimePrepare2Round;
    private	int				mTimeOneStimulus;
    private int			mTimeRest2Prepare;

    private ArrayList<Integer>		mStimSeq;
    private ArrayList<Integer>		mStimSeqOld;

    NetStim                 mNetStim;
    NetReader				mNetReader;
    EEGAlgorithm			mAlgorithm;

    public MainActivity() {
        mMargin = 40;
        mNumTarget = 12;
        mNumStimuli = 40;
        mMaxTrial = 80;
        mTimerFSM = null;
        mThreadData = null;
        mRunState = FSMSTATE.STATE_START;
        mCurrentTrial = 0;
        mWaitForNextTrial = true;

        mTimeStart2Prepare = 2000;
        mTimePrepare2Round = 2000;
        mTimeOneStimulus	= 30;
        mTimeRest2Prepare	= 2000;

        mStimSeq = new ArrayList();
        mStimSeqOld = new ArrayList();

        mNetStim = null;
        mNetReader = null;
        mAlgorithm = new EEGAlgorithm();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // Keep the screen always bright.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mSipAddress = findViewById(R.id.sipAddress);

        TableLayout pad = findViewById(R.id.dialPad);
        pad.setStretchAllColumns(true);

        // Dial Pad Layout
        int numRows = 3;
        int numCols = (int)Math.ceil(((float)mNumTarget)/numRows);
        int MP = TableLayout.LayoutParams.MATCH_PARENT;

        // The text on the Dial Pad Button
        items = this.getResources().getStringArray(R.array.strMatrix);

        // Create a Dial Pad
        mDialPad = new FlashButton[mNumTarget];
        for (int row = 0; row < numRows; row++)
        {
            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new TableLayout.LayoutParams(MP, MP, 1.0f));
            for (int col = 0; col < numCols; col++)
            {
                int index = row*numCols + col;
                if (index >= mNumTarget) break;
                mDialPad[index] = new FlashButton(this);
                mDialPad[index].setText(items[index]);
                mDialPad[index].setLayoutParams(new TableRow.LayoutParams(0, MP, 1.0f));
                mDialPad[index].setPadding(mMargin, mMargin, mMargin, mMargin);
                tr.addView(mDialPad[index]);
            }
            pad.addView(tr);
        }

        // Initialize the StimGenerator & Scan Server
        mNetStim = new NetStim(this, R.style.netstim);
        mNetReader = new NetReader(this, R.style.netreader);
        mAlgorithm.setModelPath(Environment.getExternalStorageDirectory()+getString(R.string.model_path)+getString(R.string.model_file));

        if (SipManager.isVoipSupported(this) && SipManager.isApiSupported(this)) {
            showToast("Congrats, your Device made it!");
        } else {
            showToast("Not supported");
        }

        // Get the Instance of the SipSetApplication
        sipSet = (SipSetApplication) getApplication();

        // Initialize the SipSetting
        initializeSipSetting();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Create Menu Items
        getMenuInflater().inflate(R.menu.menu_context, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_start:
                start();
                break;
            case R.id.action_stop:
                stop();
                break;
            case R.id.action_netreader:
                mNetReader.show();
                break;
            case R.id.action_netstim:
                mNetStim.show();
                break;
            case R.id.action_setting:
                updateSettings();
                break;
        }
        return true;
    }

    @Override
    public void run() {
        while (!mUserStop) {
            checkDataReady();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (call != null) {
            call.close();
        }
        new SipSetting().closeSipProfile();
    }

    public void start() {
        if (!initSystem()) return;
        mAlgorithm.call(EEGAlgorithm.JOB_INIT);

        mUserStop = false;
        mRunState = FSMSTATE.STATE_START;
        setTimerFSM(mRunState, 10);
        startDataThread();
    }

    public void stop()
    {
        mUserStop = true;
    }

    // Initialize the BCI System
    public boolean initSystem() {
        // Checks if the StimGenerator is connected.
        if (!mNetStim.isConnected()) {
            mNetStim.show();
            (new AlertDialog.Builder(this).
                    setMessage("Please connect to the StimGenerator, then press OK")
            ).show();
            return false;
        }
        // Checks if the Scan server is connected.
        if (!mNetReader.isConnected()) {
            mNetReader.show();
            (new AlertDialog.Builder(this).
                    setMessage("Please start the Scan server, then press OK")
            ).show();
            return false;
        }
        return mNetReader.isConnected();
    }


    public void checkDataReady() {
        if (mUserStop) {
            cancelTimerFSM();
            return;
        }

        Epoch epoch = mNetReader.getEpoch();
        if (epoch != null) {
            int event = epoch.event;
            if (event >= EVENT.DATA_BEGIN && event <= EVENT.DATA_END) {
                System.out.println("Received event: " + event);
                if (mWaitForNextTrial) {
                    return;
                }

                mAlgorithm.setInput(epoch);
                mAlgorithm.call(EEGAlgorithm.JOB_ROUND);

                int result = mAlgorithm.getResult();
                if (result >= 0 && result < mNumTarget) {
                    feedback(result);
                    mAlgorithm.resetResult();
                }
            }
        }
    }

    protected void feedback(int result) {
        mWaitForNextTrial = true;
        mCurrentTrial ++;

        if (result == mNumTarget - 2) {
            delete();
        } else if (result == mNumTarget -1) {
            makeOutgoingCall();
        } else {
            Message msg = new Message();
            msg.what = result;
            mFeedbackHandler.sendMessage(msg);
        }

        setTimerFSM(FSMSTATE.STATE_PREPARE, mTimeRest2Prepare);
    }

    private Handler mFeedbackHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (mSipAddress != null) {
                String strResult = mSipAddress.getText().toString();
                strResult += items[msg.what];
                mSipAddress.setText(strResult);
                mSipAddress.invalidate();
            }
        }
    };

    protected void delete() {
        if (mSipAddress != null) {
            String strResult = mSipAddress.getText().toString();
            strResult = strResult.substring(0, strResult.length() );
            mSipAddress.setText(strResult);
            mSipAddress.invalidate();
        }
    }

    public void startDataThread() {
        mThreadData = new Thread(this);
        mThreadData.start();
    }

    public void setTimerFSM(final FSMSTATE state, int delay) {
        synchronized (this) {
            if (mTimerFSM != null) {
                mTimerFSM.cancel();
                mTimerFSM = null;
            }

            mTimerFSM = new Timer(true);
            mTimerFSM.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FSM(state);
                        }
                    });
                }
            }, delay);
        }
    }

    public void cancelTimerFSM() {
        synchronized (this) {
            if (mTimerFSM != null) {
                mTimerFSM.cancel();
                mTimerFSM = null;
            }
        }
    }

    public void FSM(FSMSTATE state) {
        if (mUserStop) {
            mTimerFSM.cancel();
            mTimerFSM = null;
            return;
        }

        switch(state) {
            case STATE_START:
                if (mStimSeqOld == null)
                    mStimSeqOld = new ArrayList<>();
                else
                    mStimSeqOld.clear();
                setTimerFSM(FSMSTATE.STATE_PREPARE, mTimeStart2Prepare);
                break;
            case STATE_PREPARE:
                mStimIndex = 0;
                mRoundIndex = 0;
                mNetStim.outputEvent(EVENT.START);
                setTimerFSM(FSMSTATE.STATE_ROUNDINIT, mTimePrepare2Round);
                break;
            case STATE_ROUNDINIT:
                mWaitForNextTrial = false;
                mStimSeq = generateSeq(mStimSeqOld, 40, 20);
                mStimSeqOld = (ArrayList)mStimSeq.clone();
                System.out.print("Trial: "+mCurrentTrial+", Round: "+mRoundIndex+", Seq: ");
                displaySeq(mStimSeq);

                if (mRoundIndex > 8) {
                    System.out.println("xxxx");
                }

                int mFlashIndex = mStimSeq.remove(0);
                mNetStim.outputEvent(EVENT.DATA_BEGIN+mFlashIndex-1);
                mDialPad[mFlashIndex-1].FlashOnce(100);
                mStimIndex ++;

                if (!mWaitForNextTrial) setTimerFSM(FSMSTATE.STATE_ROUND, mTimeOneStimulus);
                break;
            case STATE_ROUND:
                if (mWaitForNextTrial) break;
                mFlashIndex = mStimSeq.remove(0);
                mNetStim.outputEvent(EVENT.DATA_BEGIN+mFlashIndex-1);
                mDialPad[mFlashIndex-1].FlashOnce(100);
                mStimIndex ++;

                if (mStimIndex >= mNumStimuli)
                {
                    mStimIndex = 0;
                    mRoundIndex ++;

                    if (!mWaitForNextTrial) setTimerFSM(FSMSTATE.STATE_ROUNDINIT, mTimeOneStimulus);
                    break;
                }

                if (!mWaitForNextTrial)setTimerFSM(FSMSTATE.STATE_ROUND, mTimeOneStimulus);
                break;
            case STATE_REST:
                mCurrentTrial ++;
                setTimerFSM(FSMSTATE.STATE_PREPARE, mTimeRest2Prepare);
                break;
        }

        if (mCurrentTrial > mMaxTrial) {
            stop();
        }
    }

    public int rand(int n) {
        Random rand = new Random(System.nanoTime());
        return rand.nextInt(n);
    }

    public int[] randperm(int n) {
        int i,k;
        int tmp[] = new int[n];
        int perm[] = new int[n];

        for(i=0; i<n; i++)
        {
            tmp[i] = i+1;
        }

        Random rand = new Random(System.nanoTime());
        for(i=0; i<n; i++)
        {
            k = rand.nextInt(n-i);
            perm[i] = tmp[k];
            tmp[k] = tmp[n-i-1];
        }
        return perm;
    }

    public ArrayList generateSeq(ArrayList oldSeq, int len, int interval) {
        int i,j;
        ArrayList<Integer> newSeq;
        if (oldSeq.size() >0) {
            newSeq = new ArrayList<>();
            ArrayList fixSeq = new ArrayList();
            for(i=0; i<len; i++) {
                fixSeq.add(i+1);
            }

            for(i=0; i<interval; i++) {
                boolean valid;

                int new_temp;
                int index_temp;
                do {
                    valid = true;
                    index_temp = rand(fixSeq.size());
                    new_temp = (Integer)fixSeq.remove(index_temp);

                    for(j=0; j<interval-i; j++) {
                        int old_temp = (Integer)oldSeq.get(len-j-1);
                        if(old_temp == new_temp) {
                            fixSeq.add(index_temp, new_temp);
                            valid = false;
                            break;
                        }
                    }
                } while(!valid);

                newSeq.add(new_temp);
            }

            for(; i<len; i++) {
                int temp = rand(fixSeq.size());
                int value = (Integer)fixSeq.get(temp);
                newSeq.add(value);
                fixSeq.remove(temp);
            }
        } else {
            int perm[] = randperm(len);
            // newSeq = new ArrayList(Arrays.asList(perm));
            newSeq = new ArrayList(perm.length);
            for (i=0; i<perm.length; i++)
            {
                newSeq.add(perm[i]);
            }
        }

        return newSeq;
    }

    public void displaySeq(ArrayList seq) {
        for (int i=0; i<seq.size(); i++)
        {
            System.out.print(seq.get(i)+" ");
        }
        System.out.print("\n");
    }

    // Initialize the SipSetting
    protected void initializeSipSetting() {

        // Obtain User's Sip Account Information
        SipManager manager = sipSet.getManager();
        SipProfile sipAccount = sipSet.getSipAccount();
        call = sipSet.getCall();

        // Checks if the SipAccount is registration.
        if (manager == null && sipAccount == null) {
            new AlertDialog.Builder(this)
                    .setMessage("Please update your SIP Account Settings.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateSettings();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Noop.
                        }
                    })
                    .show()
                    .setCanceledOnTouchOutside(false);
            return;
        }

        showToast("Server is Ready.");

    }

    // Make an outgoing call
    protected void makeOutgoingCall() {
        String sipAddress = mSipAddress.getText().toString();
        sipSet.setSipAddress(sipAddress);
        Intent outgoingCallActivity = new Intent(getBaseContext(), OutgoingCall.class);
        startActivity(outgoingCallActivity);
    }

    // Jump to activity_setting Interface
    protected void updateSettings() {
        Intent settingActivity = new Intent(getBaseContext(), SipSetting.class);
        startActivity(settingActivity);
    }

    /**
     * Show toasts with a message of the server's status
     * @param content The String to display on the toast
     */
    protected void showToast(final String content) {
        Toast toast = Toast.makeText(this, content, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

}
