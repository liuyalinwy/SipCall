package scutbci.lyl.sipcall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.sip.*;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.ParseException;

/**
 * Created by lyl.
 */

public class SipSetting extends Activity {

    // The Sip Call Info
    private SipManager manager;
    private SipProfile sipAccount;

    private EditText mUserName;
    private EditText mDomain;
    private EditText mPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Get the Instance of the SipSetApplication
        SipSetApplication sipSet = (SipSetApplication) getApplication();

        manager = sipSet.getManager();
        sipAccount = sipSet.getSipAccount();

        mUserName = findViewById(R.id.username);
        mDomain = findViewById(R.id.domain);
        mPassword = findViewById(R.id.password);
        Button mSignIn = findViewById(R.id.sign_in_button);
        Button mHide = findViewById(R.id.hide_button);

        mSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initializeManager();
            }
        });
        mHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainDialPad();
            }
        });

        // Update User's Sip Account Information
        sipSet.setSipAccount(sipAccount);
    }

    // Initialize the Sip Manager
    protected void initializeManager() {

        if (manager == null){
            showToast("Initialization failed. Please check your Device Settings.");
            return;
        }

        initializeSipProfile();

    }

    // Initialize the Sip Profile
    protected void initializeSipProfile() {

        if (sipAccount != null){
            closeSipProfile();
        }

        String username = mUserName.getText().toString();
        String domain = mDomain.getText().toString();
        String password = mPassword.getText().toString();

        if (username.length() == 0 || domain.length() == 0 || password.length() == 0) {
            new AlertDialog.Builder(this)
                    .setMessage("Please enter your SIP Account.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Noop
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mainDialPad();
                        }
                    })
                    .show();
            return;
        }

        try {
            // Build the SIP profile
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            sipAccount = builder.build();

            // Register a pending intent for incoming calls
            Intent i = new Intent();
            i.setAction("android.intent.action.SipSetting");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            manager.open(sipAccount, pi, null);

            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.
            manager.setRegistrationListener(sipAccount.getUriString(),
                    new SipRegistrationListener() {
                        public void onRegistering(String localProfileUri) {
                            showToast("Registering with SIP Server...");
                        }
                        public void onRegistrationDone(String localProfileUri, long expiryTime) {
                            showToast("Server is Ready.");
                            mainDialPad();
                        }
                        public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                         String errorMessage) {
                            showToast("Registration failed. Please check your SipAccount.");
                        }
            });
        } catch (ParseException | SipException e) {
            showToast("Connection Error.");
        }

    }

    // Back to activity_main Interface
    protected void mainDialPad() {
        Intent mainActivity = new Intent(getBaseContext(), MainActivity.class);
        startActivity(mainActivity);
    }

    /**
     * Closes out your Sip profile, freeing associated objects into memory
     * and logoff your device from the server.
     */
    protected void closeSipProfile() {
        if (manager == null) {
            return;
        }
        try {
            if (sipAccount != null) {
                manager.close(sipAccount.getUriString());
            }
        } catch (Exception ee) {
            Log.d("onDestroy", "Failed to close Sip Profile.", ee);
        }
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