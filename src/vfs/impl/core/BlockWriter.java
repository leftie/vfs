package vfs.impl.core;

import net.jcip.annotations.NotThreadSafe;
import vfs.exception.VFSException;

import java.io.IOException;
import java.io.OutputStream;

@NotThreadSafe
public class BlockWriter {
    private final OutputStream stream;
    private final int blockNo;

    BlockWriter(final OutputStream stream, final int blockNo) {
        this.stream = stream;
        this.blockNo = blockNo;
    }

    public int close() {
        try {
            stream.close();
        } catch (IOException e) {
            throw new VFSException(e);
        }

        return blockNo;
    }

    public BlockWriter write(final byte[] bs) {
        for (final byte b : bs) {
            write(b);
        }
        return this;
    }


    public BlockWriter write(final byte b) {
        try {
            stream.write(b);
        } catch (IOException e) {
            throw new VFSException(e);
        }
        return this;
    }

    public OutputStream asStream() {
        return stream;
    }
}
