package vfs.impl.core;

import net.jcip.annotations.NotThreadSafe;

import java.io.OutputStream;
import java.nio.ByteBuffer;

@NotThreadSafe
public class ByteBufferByteSinkAndSrc implements ByteSink, ByteSrc {

    private ByteBuffer target;

    public ByteBufferByteSinkAndSrc(final ByteBuffer target) {
        this.target = target;
    }

    @Override
    public void flush() {
        //doNothing
    }

    @Override
    public void close() {
        target = null;
    }

    @Override
    public OutputStream openOut(final long pos) {
        return new OutputStream() {
            int writeCnt = 0;
            @Override
            public void write(final int b) {
                if (pos >= Integer.MAX_VALUE) {
                    throw new RuntimeException();
                }
                target.put((int) pos + writeCnt++, (byte) (b & 0xFF));
            }
        };
    }

    @Override
    public byte[] read(final long from, final int length) {
        final byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = target.get(i + (int) from);
        }
        return out;
    }
}
