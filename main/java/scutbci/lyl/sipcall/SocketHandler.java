package scutbci.lyl.sipcall;

import java.nio.ByteBuffer;

public interface SocketHandler {
    void onReceive(IOBuffer buf);
    void onDisconnect();
}

class IOBuffer {
    private int mLimit;
    private ByteBuffer mBuffer;

    private int INCREASE_SIZE = 4096;

    public IOBuffer() {
        mLimit = INCREASE_SIZE;
        mBuffer = ByteBuffer.allocate(mLimit);
    }

    public IOBuffer(int limit) {
        mLimit = limit;
        mBuffer = ByteBuffer.allocate(mLimit);
    }

    public int position()
    {
        return mBuffer.position();
    }

    public int write(byte[] buf)
    {
        return write(buf, 0, buf.length);
    }

    public int write(byte[] buf, int offset, int len)
    {
        synchronized (this) {
            int writelen = len;
            if (len > mBuffer.remaining()) {
                mLimit = mBuffer.capacity()+ (int) (Math.ceil((double) (len - mBuffer.remaining()) / INCREASE_SIZE) * INCREASE_SIZE);
                System.out.println("Reallocate memory for IOBuffer to size: "+mLimit);
                ByteBuffer bufTemp = ByteBuffer.allocate(mLimit);
                mBuffer.flip();
                bufTemp.put(mBuffer);
                mBuffer.clear();
                mBuffer = bufTemp.duplicate();
            }

            try {
                mBuffer.put(buf, offset, writelen);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return writelen;
        }
    }

    public int read(byte[] buf)
    {
        return read(buf, 0, buf.length);
    }

    public int read(byte[] buf, int offset, int len)
    {
        synchronized (this) {
            int readlen = len;
            if (len > position()) {
                readlen = position();
            }

            mBuffer.flip();
            mBuffer.get(buf, offset, readlen);

            ByteBuffer bbufTemp = ByteBuffer.allocate(mLimit);
            bbufTemp.put(mBuffer);
            mBuffer.clear();
            mBuffer = bbufTemp.duplicate();

            return readlen;
        }
    }

    public void clear()
    {
        synchronized (this) {
            mBuffer.clear();
        }
    }
}
