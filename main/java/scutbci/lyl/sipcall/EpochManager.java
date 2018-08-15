package scutbci.lyl.sipcall;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

interface EpochHandler
{
    void onDataReady(Epoch epoch);
}

class Epoch {
    public int event;
    public int channels;
    public int epochlen;
    public float[] data;
    private int pointindex;

    public Epoch(int channels, int epochlen) {
        init(channels, epochlen);
    }

    public Epoch(int channels, int epochlen, int event, float[] data) {
        init(channels, epochlen);
        this.event = event;
        setData(data, data.length);
    }

    public void init(int channels, int epochlen)
    {
        this.channels = channels;
        this.epochlen = epochlen;

        this.event = 0;
        if (this.channels*this.epochlen > 0)
            this.data = new float[channels*epochlen];
        else
            this.data = null;

        this.pointindex = 0;
    }

    public void setData(float[] data, int len) {
        if (this.data == null) this.data = new float[channels*epochlen];

        int datalen = Math.min(len, channels*epochlen);
        System.arraycopy(data, 0, this.data, 0, datalen);
    }

    public void insertSample(float[] data)
    {
        if (pointindex < epochlen) {
            for (int ch=0; ch<channels; ch++)
            {
                this.data[pointindex*channels+ch] = data[ch];
            }
            pointindex += 1;
        }
    }

    public boolean isFull()
    {
        return ((pointindex+1) >= epochlen);
    }

    public void write(OutputStream os) throws IOException
    {
        if (isFull()) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            for (int i=0; i<epochlen; i++)
            {
                for (int j=0; j<channels; j++)
                {
                    bw.write(String.format("%.4f, ",data[i*channels+j]));
                }
                bw.write("\n");
            }
            bw.flush();
        }
    }

    public void read(InputStream is) throws IOException
    {
        if (this.data == null) this.data = new float[channels*epochlen];
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        int i = 0;
        String strLine = null;
        while ((strLine = br.readLine()) != null) {
            String[] strData = strLine.split("\\d");
            for (int j=0; j<strData.length; j++)
            {
                data[i*channels+j] = Float.parseFloat(strData[j]);
            }
            i++;
        }
    }
}

public class EpochManager {

    private int mEpochLen;
    private int mEEGChannels;
    private int mEventChannels;
    private int[] mEventQueue;
    private int mMaxEpochs;
    private LinkedList<Epoch> mEpochQueue;
    private EpochHandler mEpochHandler;

    public EpochManager(EpochHandler handler) {
        this.mEpochHandler = handler;

        mEpochLen = 150;
        mEEGChannels = 0;
        mEventChannels = 1;
        mEventQueue = new int[4];
        mMaxEpochs = 100;
        mEpochQueue = new LinkedList<Epoch>();
    }

    public void setChannelInfo(int eegch, int eventch, int epochlen)
    {
        clearEpochs();
        this.mEEGChannels = eegch;
        this.mEventChannels = eventch;
        this.mEpochLen = epochlen;
    }

    public boolean addEpoch(Epoch epoch)
    {
        synchronized (this) {
            return mEpochQueue.offer(epoch);
        }
    }

    public Epoch removeEpoch()
    {
        synchronized (this) {
            return mEpochQueue.poll();
        }
    }

    public Epoch getEpoch(boolean bRemove)
    {
        Epoch epoch = null;
        synchronized (this) {
            int size = mEpochQueue.size();
            for (int i = 0; i < size; i++) {
                epoch = mEpochQueue.get(i);
                if (epoch.isFull()) {
                    if (bRemove) mEpochQueue.remove(i);
                    return epoch;
                }
            }
        }
        return null;
    }

    public void clearEpochs()
    {
        synchronized (this) {
            while (mEpochQueue.size() > 0) {
                Epoch epoch = mEpochQueue.poll();
                epoch = null;
            }
        }
    }

    public void insertData(byte[] data, int points, boolean b32bit)
    {
        int eventCode = 0;
        float sliceData[] = new float[mEEGChannels];
        ByteBuffer bufData = ByteBuffer.wrap(data);
        bufData.order(ByteOrder.LITTLE_ENDIAN);
        bufData.rewind();

        for (int i=0; i<points; i++)
        {
            for (int ch=0; ch<mEEGChannels; ch++)
            {
                if (b32bit) {
                    sliceData[ch] = bufData.getInt();
                }
                else {
                    sliceData[ch] = bufData.getShort();
                }
            }

            if (b32bit) {
                eventCode = bufData.getInt();
            }
            else {
                eventCode = bufData.getShort();
            }

            eventCode &= 0xFF;
            if (eventCode > 0 && isAriseEvent(eventCode)) {
                Epoch epoch = new Epoch(mEEGChannels, mEpochLen);
                epoch.event = eventCode;
                addEpoch(epoch);
                // System.out.println("EpochQueue("+mEpochQueue.size()+") added a new epoch event("+eventCode+")");
            }

            // update all the epoch items
            synchronized (this) {
                int size = mEpochQueue.size();
                for (int index=0; index<size; index++)
                {
                    Epoch epoch = mEpochQueue.get(index);
                    epoch.insertSample(sliceData);

                    if (epoch.isFull()) {
                        mEpochHandler.onDataReady(epoch);
                    }
                }
            }
        }
        sliceData = null;

        // remove epochs if its size is large than 100
        if (mEpochQueue.size() > mMaxEpochs)
        {
            Epoch epoch = removeEpoch();
            epoch = null;
        }
    }

    public boolean isAriseEvent(int event)
    {
        boolean bArise = false;

        mEventQueue[3] = mEventQueue[2];
        mEventQueue[2] = mEventQueue[1];
        mEventQueue[1] = mEventQueue[0];
        mEventQueue[0] = event;

        boolean temp1 = (mEventQueue[0]==0x00);
        boolean temp2 = (mEventQueue[0]==0xFF);

        if (!temp1 && !temp2) {
            bArise = !((mEventQueue[0] == mEventQueue[1]) |
                       (mEventQueue[0] == mEventQueue[2]) |
                       (mEventQueue[0] == mEventQueue[3]));
        }
        return bArise;
    }
}
