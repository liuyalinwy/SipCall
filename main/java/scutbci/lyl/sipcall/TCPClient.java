package scutbci.lyl.sipcall;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class TCPClient extends Thread {

	private Socket mSocket = null;
    private IOBuffer mBuffer = new IOBuffer(4096);
    private boolean mRunning = false;
    private SocketHandler clientHandler = null;
	
	public TCPClient(SocketHandler ch)
	{
        clientHandler = ch;
	}
	
	public void setHandler(SocketHandler ch)
	{
        clientHandler = ch;
	}
	
	public Socket getSocket()
	{
		return mSocket;
	}
	
	public boolean connect(String host, int port)
	{
		try {
			mSocket = new Socket(host, port);
            if (isConnected()) {
                // !!! Very important for event sending
                mSocket.setTcpNoDelay(true);
                System.out.println("Connected to "+host+":"+port);
                start();
                return true;
            } else {
                System.out.println("Connect to "+host+":"+port+" failed");
                return false;
            }
		} catch(Exception e) {
			e.printStackTrace();
            return false;
		}
	}
	
	public void disconnect()
	{
		try {
            mRunning = false;
            this.interrupt();
			if (mSocket != null)
			{
				mSocket.close();
				mSocket = null;
			}
            System.out.println("Disconnected from server");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    public boolean isConnected()
    {
        return mSocket.isConnected();
    }

    public boolean isRunning()
    {
        return mRunning;
    }
	
	public void send(byte[] buff)
	{
		send(buff, buff.length);
	}
	
	public void send(byte[] buff, int len)
	{
        if (mRunning) {
            try {
                OutputStream os = mSocket.getOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(os);
                bos.write(buff, 0, len);
                bos.flush();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
	}

    public void run()
    {
        try {
            int len = -1;
            byte[] buf = new byte[4096];
            InputStream is = mSocket.getInputStream();

            mRunning = true;
            while(mRunning)
            {
                len = is.read(buf);
                if (len >0) {
                    mBuffer.write(buf, 0, len);
                    clientHandler.onReceive(mBuffer);
                } else {
                    break;
                }
            }

            clientHandler.onDisconnect();
            is.close();
        } catch(SocketException e) {
            clientHandler.onDisconnect();
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.out.println("WorkThread in TCPClient ended");
    }
}