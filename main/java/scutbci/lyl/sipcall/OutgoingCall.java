package scutbci.lyl.sipcall;

import android.app.Activity;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by lyl.
 */

public class OutgoingCall extends Activity {

    // The Sip Call Info
    private SipManager manager;
    private SipProfile sipAccount;
    private SipAudioCall call;
    private String sipAddress;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_call);

        ImageView contactPicture = findViewById(R.id.contact_picture);

        ImageView hangUp = findViewById(R.id.call_hangup);
        hangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (call != null) {
                    try {
                        call.endCall();
                    } catch (SipException e) {
                        e.printStackTrace();
                    }
                    call.close();
                }
            }
        });

        // Get the Instance of the SipSetApplication
        SipSetApplication sipSet = (SipSetApplication) getApplication();

        // Obtain User's Sip Account Information
        manager = sipSet.getManager();
        sipAccount = sipSet.getSipAccount();
        call = sipSet.getCall();
        sipAddress = sipSet.getSipAddress();

        // Initialize the Call Activity
        initiateCall();

        if (call == null) {
            return;
        } else if (call.isMuted()) {
            call.toggleMute();
        }

        sipSet.setCall(call);
    }

    /**
     * Make an outgoing call.
     */
    protected void initiateCall() {

        if (manager == null) {
            showToast("Initialization failed. Please check your Device Settings.");
        } else if (sipAccount == null) {
            showToast("Please enter your SIP Account.");
        } else if (sipAddress == null) {
            showToast("Please enter the Phone Number you want to call.");
        }

        updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.

                @Override
                public void onCalling(SipAudioCall call) {
                    //Calling UI
                }

                @Override
                public void onCallBusy(SipAudioCall call) {
                    //Busy UI?
                }

                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    if (call.isMuted()) {
                        call.toggleMute();
                    }
                    updateStatus(sipAddress + "\n" + "Calling...");
                    //updateCall(call);
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    updateStatus("Call Ended.");
                    mainDialPad();
                }
            };

            call = manager.makeAudioCall(sipAccount.getUriString(), sipAddress, listener, 30);

        } catch (Exception e) {
            Log.i("InitiateCall", "Error when trying to close manager.", e);
            if (sipAccount != null) {
                try {
                    manager.close(sipAccount.getUriString());
                }
                catch (Exception ee) {
                    Log.i("InitiateCall", "Error when trying to close manager.", ee);
                    ee.printStackTrace();
                }
            }
            if (call != null) {
                call.close();
            }
        }
    }

    /**
     * Updates the status box with the phone number of the current call.
     * @param call The current, active call.
     */
    protected void updateCall(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if(useName == null) {
            useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }

    /**
     * Updates the status box at the top of the UI with a message of your choice.
     * @param status The String to display in the status box.
     */
    protected void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = findViewById(R.id.statusLabel);
                labelView.setText(status);
            }
        });
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

    // Back to activity_main Interface
    protected void mainDialPad() {
        Intent mainActivity = new Intent(getBaseContext(), MainActivity.class);
        startActivity(mainActivity);
    }

}
