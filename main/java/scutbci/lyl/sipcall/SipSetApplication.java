package scutbci.lyl.sipcall;

import android.app.Application;
import android.net.sip.SipAudioCall;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;

/**
 * lyl.
 */

public class SipSetApplication extends Application {

    private SipManager manager = null;
    private SipProfile sipAccount = null;
    private SipAudioCall call = null;
    private String sipAddress = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void setSipAccount(SipProfile sipAccount) {
        this.sipAccount = sipAccount;
    }

    public void setCall(SipAudioCall call) {
        this.call = call;
    }

    public void setSipAddress(String phoneNum) {
        this.sipAddress = phoneNum;
    }

    public SipManager getManager() {
        if (manager == null) {
            manager = SipManager.newInstance(this);
        }
        return manager;
    }

    public SipProfile getSipAccount() {
        return sipAccount;
    }

    public SipAudioCall getCall() {
        return call;
    }

    public String getSipAddress() {
        return sipAddress;
    }

}
