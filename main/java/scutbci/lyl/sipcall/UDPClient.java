package scutbci.lyl.sipcall;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class UDPClient extends Thread {
    private int mPort = 5000;
    private String mServer = "127.0.0.1";
    private DatagramSocket mSocket = null;
    private IOBuffer mBuffer = new IOBuffer(4096);
    private boolean mRunning = false;
    private SocketHandler clientHandler = null;

    public UDPClient() {
        try {
            mSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public UDPClient(SocketHandler ch) {
        try {
            mSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        clientHandler = ch;
    }

    public boolean connect(String host, int port) {
        mPort = port;
        mServer = host;
        start();
        return true;
    }

    public void send(byte[] buff)
    {
        send(buff, buff.length);
    }

    public void send(byte[] buff, int len)
    {
        if (mRunning) {
            try {
                DatagramPacket dp = new DatagramPacket(buff, len, InetAddress.getByName(mServer), mPort);
                mSocket.send(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run()
    {
        try {
            int len = -1;
            byte[] buf = new byte[4096];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);

            mRunning = true;
            while(mRunning)
            {
                Arrays.fill(buf, (byte)0);
                mSocket.receive(dp);
                if (dp.getLength() > 0)
                {
                    mBuffer.write(dp.getData(), 0, dp.getLength());
                    clientHandler.onReceive(mBuffer);
                } else {
                    mSocket.close();
                    break;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("WorkThread in ClientSocket ended");
    }

    public void disconnect()
    {

    }

    public boolean isConnected()
    {
        return true;
    }

    public boolean isRunning()
    {
        return mRunning;
    }
}
