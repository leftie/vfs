package vfs.impl.core;

import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@NotThreadSafe
public class BBByteSinkAndSrc implements ByteSink, ByteSrc {

    private ByteBuffer bb;

    public BBByteSinkAndSrc(final ByteBuffer bb) {
        this.bb = bb;
    }

    @Override
    public void flush() {
        //doNothing
    }

    @Override
    public void close() {
        bb = null;
    }

    @Override
    public OutputStream openOut(final long pos) {
        return new OutputStream() {
            int writeCnt = 0;
            @Override
            public void write(final int b) throws IOException {
                if (pos >= Integer.MAX_VALUE) {
                    throw new RuntimeException();
                }
                bb.put((int) pos + writeCnt++, (byte) (b & 0xFF));
            }
        };
    }

    @Override
    public byte[] read(final long from, final int length) {
        final byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = bb.get(i + (int) from);
        }
        return out;
    }
}
