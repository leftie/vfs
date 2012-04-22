package vfs.impl.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vfs.exception.VFSException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BlockDevice {
    private static final Logger log = LoggerFactory.getLogger(BlockDevice.class);

    public static final int MIN_BLOCK_SIZE = 1024;

    private static final byte[] ZERO_BYTES = new byte[0];

    private final int blockSize;
    private final ByteSink sink;
    private final ByteSrc src;
    private final BlockAllocator alloc;

    public BlockDevice(final int blockSize, final ByteSink sink, final ByteSrc src, final BlockAllocator alloc) {
        this.blockSize = blockSize;
        this.alloc = alloc;
        this.sink = sink;
        this.src = src;
    }

    public BlockWriter openWriter(final int blockNo) {
        log.debug("openWriter({})", blockNo);
        final int startBlock;
        if (blockNo < 0) {
            startBlock = alloc.allocAnywhere(1);
        } else {
            if (alloc.isFree(blockNo)) {
                throw new RuntimeException("writer requested on block " + blockNo + ", but block is free");
            }
            startBlock = blockNo;
            final int blanked = blankTail(blockNo);
            if (log.isDebugEnabled()) {
                log.debug("blanked {} blocks of tail", blanked);
            }
        }
        return new BlockWriter(new BlockWritingOutputStream(startBlock, blockSize, alloc, sink), startBlock);
    }

    public BlockWriter openAppender(final int startBlockNo) {
        final Block toAppendTo = last(startBlockNo);
        log.debug("to append to " + toAppendTo.getNo());
        return new BlockWriter(
                new BlockWritingOutputStream(toAppendTo.getNo(), blockSize, alloc, sink, toAppendTo.getData()),
                startBlockNo
        );
    }

    public void touch(final int blockNo) {
        log.debug("touch({})", blockNo);
        final OutputStream output = sink.openOut(offset(blockNo));
        try {
            Block.encode(output, new Block(blockNo, 0, ZERO_BYTES), blockSize);
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                throw new VFSException(e);
            }
        }
        log.debug("touch({}) done", blockNo);
    }

    Block last(final int startBlock) {
        Block out;
        int next = startBlock;
        do {
            out = readBlock(next);
            next = out.getNext();
        } while (next > 0);
        if (log.isInfoEnabled()) {
            log.debug("last({}) is {}", startBlock, out.getNo());
        }
        return out;
    }

    public int freeStartingWith(final int blockNo) {
        if (alloc.isFree(blockNo)) {
            throw new IllegalArgumentException(blockNo + " is already free");
        }
        alloc.free(blockNo);
        return 1 + blankTail(blockNo);
    }

    private int blankTail(final int blockNo) {
        log.debug("blankTail({})", blockNo);
        int toBlank = readPossiblyEmptyBlock(blockNo).getNext();
        int blanked = 0;
        while (toBlank > 0) {
            alloc.free(toBlank);
            blanked++;
            toBlank = readPossiblyEmptyBlock(toBlank).getNext();
        }
        return blanked;
    }

    public BlockWriter openWriter() { //todo: is it really needed?
        return openWriter(-1);
    }

    long offset(final int blockNo) {
        return offset(blockNo, (long) blockSize);
    }

    static long offset(final int blockNo, final long blockSize) {
        return blockSize * blockNo;
    }

    public BlockReader openReader(final int blockNo) {
        if (alloc.isFree(blockNo)) {
            throw new IllegalStateException("reader for block " + blockNo + " required, but block is marked as free");
        }
        return new BlockReader(new BlockReadingInputStream(blockNo));
    }

    private Block readPossiblyEmptyBlock(final int blockToRead) {
        return readBlock(blockToRead, true);
    }

    private Block readBlock(final int blockToRead) {
        return readBlock(blockToRead, false);
    }

    private Block readBlock(final int blockToRead, final boolean allowEmpty) {
        final byte[] bytesWithBlock = src.read(offset(blockToRead), blockSize);
        final Block block = Block.decode(
                new ByteArrayInputStream(bytesWithBlock),
                blockSize
        );
        if (!block.isClear() || !allowEmpty) {
            assert blockToRead == block.getNo();
        }
        return block;
    }

    public void close() {
        src.close();
        sink.close();
    }

    private static class BlockWritingOutputStream extends OutputStream {

        byte[] data;
        int currentPos;
        int currentBlockNo;
        private final int blockSize;
        private final BlockAllocator alloc;
        private final ByteSink sink;

        int totalWritten = 0;
        private final int startBlock;

        private BlockWritingOutputStream(final int startBlock, final int blockSize, final BlockAllocator alloc, final ByteSink sink, final byte[] startData) {
            this.blockSize = blockSize;
            this.alloc = alloc;
            this.sink = sink;
            data = new byte[Block.calcUsefulPayload(blockSize)];
            if (startData.length > 0) {
                System.arraycopy(startData, 0, data, 0, startData.length);
            }
            currentPos = startData.length;
            this.startBlock = startBlock;
            currentBlockNo = startBlock;
        }

        private BlockWritingOutputStream(final int startBlock, final int blockSize, final BlockAllocator alloc, final ByteSink sink) {
            this(startBlock, blockSize, alloc, sink, ZERO_BYTES);
        }

        @Override
        public void write(final int b) throws IOException {
            if (currentPos == data.length) {
                currentBlockNo = this.doFlush(true);
                currentPos = 0;
            }
            data[currentPos++] = (byte) (b & 0xff);
            totalWritten++;
        }

        @Override
        public void flush() throws IOException {
            super.flush();
            this.doFlush(false);
        }

        private int doFlush(final boolean hasNext) throws IOException {
            final int nextBlockNo = hasNext ? alloc.allocNextTo(currentBlockNo) : 0;
            log.debug("flushing block #{} with next {}", currentBlockNo, nextBlockNo);
            final OutputStream out = sink.openOut(offset(currentBlockNo, (long) blockSize));
            try {
                final Block block = new Block(currentBlockNo, nextBlockNo, data, currentPos);
                Block.encode(out, block, blockSize);
            } finally {
                out.close();
            }
            return nextBlockNo;
        }

        @Override
        public void close() throws IOException {
            this.flush();
            super.close();
        }
    }

    private class BlockReadingInputStream extends InputStream {
        byte[] data;
        int currentPos;
        int nextBlock;

        private final int startBlockNo;
        private int totalRead;

        private BlockReadingInputStream(final int blockNo) {
            nextBlock = blockNo;
            startBlockNo = blockNo;
            readNextBlock();
        }

        private void readNextBlock() {
            log.debug("readNextBlock({})", nextBlock);
            final Block block = readBlock(nextBlock);
            this.data = block.getData();
            this.nextBlock = block.getNext();
            this.currentPos = 0;
        }

        @Override
        public int read() {
            if (data.length == 0) {
                return -1;
            }
            if (currentPos >= data.length) {
                if (nextBlock == 0) {
                    return -1;
                } else {
                    readNextBlock();
                }
            }
            totalRead++;
            return data[currentPos++] & 0xff;
        }

        @Override
        public void close() throws IOException {
            log.debug("total read " + totalRead + " starting with block " + startBlockNo);
            super.close();
        }
    }
}
