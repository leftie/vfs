package vfs.impl.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

class Block {
    private static final Logger log = LoggerFactory.getLogger(Block.class);

    private final int no;
    private final int next;
    private final byte[] data;

    private static final int WORD_SIZE = 8;

    Block(final int no, final int next, final byte[] data) {
        this(no, next, data, data.length);
    }

    Block(final int no, final int next, final byte[] data, final int dataLength) {
        this.no = no;
        this.next = next;
        this.data = Arrays.copyOf(data, dataLength);
    }

    public static int calcUsefulPayload(final int blockSize) {
        return blockSize - WORD_SIZE * 3;
    }

    public static Block decode(final InputStream input, final int blockSize) {
        try {
            final byte[] data = new byte[blockSize];
            for (int i = 0; i < blockSize; i++) {
                data[i] = (byte) input.read();
            }
            final int no = readInt(data, 0);
            log.debug("decoded no {}", no);

            final int length = readInt(data, blockSize - WORD_SIZE * 2);
            log.debug("decoded length {}", length);
            if (length > blockSize) {
                throw new AssertionError("read length of " + length + " for blocksize of " + blockSize);
            }

            final int next = readInt(data, blockSize - WORD_SIZE);
            log.debug("decoded next {}", next);

            final byte[] actualData = new byte[length];
            System.arraycopy(data, WORD_SIZE, actualData, 0, length);

            return new Block(no, next, actualData);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int readInt(final byte[] bytes, final int start) {
        return (bytes[start] << 24) + (bytes[start + 1] << 16) + (bytes[start + 2] << 8) + bytes[start + 3];
    }

    private static void writeInt(final byte[] bytes, final int pos, final int value) {
        bytes[pos] = (byte) (value >>> 24);
        bytes[pos + 1] = (byte) (value >>> 16);
        bytes[pos + 2] = (byte) (value >>> 8);
        bytes[pos + 3] = (byte) value;

    }

    public static void encode(final OutputStream output, final Block b, final int blockSize) {
        if (blockSize < b.data.length + WORD_SIZE * 3) {
            throw new IllegalArgumentException("block too small for data. block size is " + blockSize + ", data size is " + b.data.length);
        }
        final byte[] out = new byte[blockSize];
        writeInt(out, 0, b.no);
        System.arraycopy(b.data, 0, out, WORD_SIZE, b.data.length);
        writeInt(out, blockSize - WORD_SIZE*2, b.data.length);
        writeInt(out, blockSize - WORD_SIZE, b.next);
        try {
            output.write(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(final Object o) { //generated
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Block block = (Block) o;

        if (next != block.next) return false;
        if (no != block.no) return false;
        if (!Arrays.equals(data, block.data)) return false;

        return true;
    }

    @Override
    public int hashCode() { //generated
        int result = no;
        result = 31 * result + next;
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    public int getNo() {
        return no;
    }

    public int getNext() {
        return next;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public String toString() {
        return "Block{" +
                "no=" + no +
                ", next=" + next +
                ", data=" + data.length +
                '}';
    }

    public boolean isClear() {
        return no == 0 && data.length == 0 && next == 0;
    }
}
