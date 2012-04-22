package vfs.impl.core;

import net.jcip.annotations.NotThreadSafe;
import vfs.exception.VFSCorruptException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.BitSet;

@NotThreadSafe
public class RAFByteSinkAndSrc implements ByteSink, ByteSrc {

    private final RandomAccessFile file;

    public RAFByteSinkAndSrc(final RandomAccessFile file) {
        this.file = file;
    }

    public BitSet loadOccupanceBitMap(final int blockSize) {
        final long blockCnt;
        try {
            blockCnt = file.length() / blockSize;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (blockCnt > Integer.MAX_VALUE) {
            throw new RuntimeException("cannot fit content of file into one bitset. required " + blockCnt);
        }
        final BitSet out = new BitSet((int) blockCnt);
        System.out.println("load occupance");
        try {
            file.seek(0);
            final byte[] buf = new byte[blockSize];
            int blockNum = 0;
            while (true) {
                final int read = file.read(buf);
                System.out.println("r" + read + " " + Arrays.toString(buf));
                if (read == blockSize) {
                    final Block block = Block.decode(new ByteArrayInputStream(buf), blockSize);
                    if (blockNum == 0 && block.getNo() == 0) {
                        out.set(blockNum);
                    } else {
                        if (!block.isClear()) {
                            assert block.getNo() == blockNum;
                            out.set(blockNum);
                        }
                    }
                    blockNum++;
                } else if (read == -1) {
                    return out;
                } else {
                    throw new VFSCorruptException("read " + read + " while expected % blocksize");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void flush() {
        //doNothing
    }

    @Override
    public void close() {
        try {
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OutputStream openOut(final long pos) {
        return new OutputStream() {
            boolean positioned = false;

            @Override
            public void write(final int b) throws IOException {
                if (!positioned) {
                    file.seek(pos);
                    positioned = true;
                }
                file.writeByte(b & 0xff);
            }
        };
    }

    @Override
    public byte[] read(final long from, final int length) {
        final byte[] out = new byte[length];
        try {
            file.seek(from);
            file.read(out);
            return out;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
