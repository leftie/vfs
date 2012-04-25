package vfs.impl.core;

import vfs.exception.VFSException;

import java.io.IOException;
import java.io.OutputStream;

public class DataOutput {
    private final OutputStream stream;
    private final int blockNo;

    DataOutput(final OutputStream stream, final int blockNo) {
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

    public DataOutput write(final byte[] bs) {
        for (final byte b : bs) {
            write(b);
        }
        return this;
    }


    public DataOutput write(final byte b) {
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
