package scutbci.lyl.sipcall;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.LinkedList;

interface MessageHandler
{
    void onCtrlMsg(AmpMessage msg);
    void onDataMsg(AmpMessage msg);
}

class CTRLCODE {
    // "CTRL" packet control codes
    final public static short GeneralControlCode  = 1;
    final public static short ServerControlCode   = 2;
    final public static short ClientControlCode   = 3;
}

class CTRLREQ {
    // "CTRL" packet requests
    final public static short RequestForVersion   = 1;
    final public static short ClosingUp            = 2;
    final public static short ServerDisconnected  = 3;

    final public static short StartAcquisition    = 1;
    final public static short StopAcquisition     = 2;
    final public static short StartImpedance      = 3;
    final public static short ChangeSetup         = 4;
    final public static short DCCorrection        = 5;

    final public static short RequestEdfHeader    = 1;
    final public static short RequestAstFile      = 2;
    final public static short RequestStartData    = 3;
    final public static short RequestStopData     = 4;
    final public static short RequestBasicInfo    = 5;
}

class DATACODE {
    // "DATA" packet codes and requests
    final public static short DataType_InfoBlock  = 1;
    final public static short DataType_EegData    = 2;
}

class DATAREQ {
    final public static short InfoType_Version    = 1;
    final public static short InfoType_EdfHeader  = 2;
    final public static short InfoType_BasicInfo  = 3;

    final public static short DataTypeRaw16bit    = 1;
    final public static short DataTypeRaw32bit    = 2;
}

class AmpMessage implements Serializable
{
    private byte[]  mId;       // ID string, no trailing '\0'
    private short   mCode;      // Code
    private short   mRequest;   // Request
    private int     mSize;      // Body size (in bytes)
    private byte[]  mData;
    private boolean mIsHeaderReady;
    private boolean mIsRecvComplete;

    public AmpMessage()
    {
        mId = new byte[4];
        mData = null;
        mIsHeaderReady = false;
        mIsRecvComplete = false;
    }

    public AmpMessage(String id, short code, short request, int size)
    {
        mId = new byte[4];
        mId[0] = (byte)id.charAt(0);
        mId[1] = (byte)id.charAt(1);
        mId[2] = (byte)id.charAt(2);
        mId[3] = (byte)id.charAt(3);
        mCode = code;
        mRequest = request;
        mSize = size;
        mData = null;
        mIsHeaderReady = false;
        mIsRecvComplete = false;
    }

    public short getCode() { return mCode; }
    public short getRequest() { return mRequest; }

    public void setHeader(byte[] buf)
    {
        ByteBuffer bbuf = ByteBuffer.wrap(buf);
        bbuf.get(mId, 0, 4);
        mCode = bbuf.getShort();
        mRequest = bbuf.getShort();
        mSize = bbuf.getInt();
        mIsHeaderReady = true;
        if (mSize <= 0) mIsRecvComplete = true;
    }

    public boolean isHeaderReady()
    {
        return mIsHeaderReady;
    }

    public void setData(byte[] data, int offset, int size)
    {
        mSize = size;
        if (size > 0)
        {
            mData = new byte[size];
            System.arraycopy(data, 0, mData, offset, size);
            mIsRecvComplete = true;
        }
    }

    public byte[] getData()
    {
        return mData;
    }

    public boolean hasData()
    {
        return (mSize > 0 && mData != null);
    }

    public boolean isRecvComplete()
    {
        return mIsRecvComplete;
    }

    public byte[] getBytes()
    {
        ByteBuffer buf = ByteBuffer.allocate(getHeaderSize()+mSize);
        buf.put(mId, 0, 4);
        buf.putShort(mCode);
        buf.putShort(mRequest);
        buf.putInt(mSize);
        if ((mSize > 0) && (mData != null))
        {
            buf.put(mData, 0, mSize);
        }
        return buf.array();
    }

    public void writeObject(ObjectOutputStream oos) throws IOException
    {
        oos.write(getBytes());
    }

    public void readObject(ObjectInputStream ois) throws IOException
    {
        byte buf[] = new byte[getHeaderSize()];
        ois.read(buf);
        setHeader(buf);
        if (mSize > 0)
        {
            mData = new byte[mSize];
            ois.read(mData);
        }
    }

    // Header portion includes mId,mCode,mRequest,mSize
    public static int getHeaderSize() { return 12; }
    public int		    getDataSize()   { return mSize; }
    public boolean	    isCtrlPacket () { return ((new String(mId)).equalsIgnoreCase("CTRL")); }
    public boolean 	isDataPacket () { return ((new String(mId)).equalsIgnoreCase("DATA")); }
};

public class AmpTransport implements SocketHandler {

    private int mPort;
    private String mServer;
    private String mStatus;
    private TCPClient mSocket;
    private AmpMessage mMsg;
    private int mMaxMessage = 1000;
    private LinkedList<AmpMessage> mMsgQueue;
    private Handler mMsgHandler;

    public AmpTransport(String server, int port) {
        mPort = port;
        mServer = server;
        mSocket = null;
        mMsgQueue = null;
    }

    public AmpTransport(String server, int port, Handler handler) {
        mPort = port;
        mServer = server;
        mSocket = null;
        mMsgQueue = null;
        setMsgHandler(handler);
    }

    public void setMsgHandler(Handler handler) {
        mMsgHandler = handler;
    }

    public boolean start()
    {
        if (mSocket != null)
        {
            mSocket.disconnect();
            mSocket = null;
        }
        mSocket = new TCPClient(this);
        if (!mSocket.connect(mServer, mPort))
        {
            return false;
        }

        if (mMsgQueue != null) {
            clearMessages();
            mMsgQueue = null;
        }
        mMsgQueue = new LinkedList<AmpMessage>();

        sendCommand(CTRLCODE.ClientControlCode, CTRLREQ.RequestBasicInfo);

        return true;
    }

    public void stop()
    {
        if (mSocket != null) {
            sendCommand(CTRLCODE.ClientControlCode, CTRLREQ.RequestStopData);
            sendCommand(CTRLCODE.GeneralControlCode, CTRLREQ.ClosingUp);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mSocket.disconnect();
        mSocket = null;

        clearMessages();
        mMsgQueue = null;
    }

    public void addMessage(AmpMessage msg)
    {
        synchronized (this)
        {
            mMsgQueue.offer(msg);
        }
    }

    public AmpMessage removeMessage()
    {
        synchronized (this)
        {
            AmpMessage msg = null;
            if (mMsgQueue.size() > 0) msg = mMsgQueue.poll();
            return msg;
        }
    }

    public void clearMessages()
    {
        synchronized (this) {
            while (mMsgQueue.size() > 0) {
                AmpMessage msg = mMsgQueue.poll();
                msg = null;
            }
        }
    }

    public void send(AmpMessage msg)
    {
        if (mSocket == null || !mSocket.isRunning()) return;	// No connection yet

        mSocket.send(msg.getBytes(), AmpMessage.getHeaderSize());
        int size = msg.getDataSize();
        if (size > 0)
        {
            mSocket.send(msg.getData(), size);
        }
    }

    public void sendCommand(short code, short request)
    {
        AmpMessage msg = new AmpMessage("CTRL", code, request, 0);
        System.out.println("|->Sent Control Packet: code="+msg.getCode()+", request="+msg.getRequest());
        send(msg);
    }

    public boolean getData(byte[] data, int size)
    {
        if (mMsgQueue == null || mMsgQueue.size() == 0) { return false; }

        AmpMessage msg = removeMessage();
        if (msg.isDataPacket()) {
            if (msg.getDataSize() != size) {
                System.out.println("AmpTransport error, wrong size "+msg.getDataSize()+", should be "+size);
                return false;
            }
            else {
                System.arraycopy(msg.getData(), 0, data, 0, size);
                return true;
            }
        }
        return false;
    }

    public void onMessage(AmpMessage msg)
    {
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putSerializable("MSG", msg);
        message.setData(bundle);

        if (msg.isCtrlPacket()) {
            System.out.println("MsgQueue("+mMsgQueue.size()
                    +")|<-Recv Control Packet: code="+msg.getCode()+", request="+msg.getRequest());
            // mMsgHandler.onCtrlMsg(msg);
            message.what = 1;
            mMsgHandler.sendMessage(message);
            msg = null;
        }
        else if (msg.isDataPacket()){
            /*
            System.out.println("MsgQueue("+mMsgQueue.size()
                    +")|<-Recv Data Packet: code="+msg.getCode()
                    +", request="+msg.getRequest()+", size="+msg.getDataSize());
            */
            if (msg.getCode() == DATACODE.DataType_EegData) {
                if (msg.getRequest() == DATAREQ.DataTypeRaw16bit || msg.getRequest() == DATAREQ.DataTypeRaw32bit) {
                    addMessage(msg);
                }
            } else {
                // mMsgHandler.onDataMsg(msg);
                message.what = 2;
                mMsgHandler.sendMessage(message);
                msg = null;
            }
        }
    }

    @Override
    public void onDisconnect() {
        System.out.println("Disconnected from server");
    }

    @Override
    public void onReceive(IOBuffer buf) {
        if ((mMsg == null) || (!mMsg.isHeaderReady())) { // 收到数据
            mMsg = new AmpMessage();
            int hdrsize = AmpMessage.getHeaderSize();
            if (buf.position() >= hdrsize) {
                byte hdr[] = new byte[hdrsize];
                buf.read(hdr, 0, hdrsize);
                mMsg.setHeader(hdr);
            }

            int datasize = mMsg.getDataSize();
            if (datasize > 0 && buf.position() >= datasize) {
                byte data[] = new byte[datasize];
                int readlen = buf.read(data, 0, datasize);
                mMsg.setData(data, 0, readlen);
            }
        }
        else {
            int datasize = mMsg.getDataSize();
            if (datasize > 0 && buf.position() >= datasize) {
                byte data[] = new byte[datasize];
                int readlen = buf.read(data, 0, datasize);
                mMsg.setData(data, 0, readlen);
            }
        }

        if ((mMsg != null) && (mMsg.isRecvComplete())) {
            onMessage(mMsg);
            mMsg = null;
        }
    }
}
