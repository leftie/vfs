package vfs.impl.core;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.io.IOUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BlockDeviceTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(BlockDeviceTest.class);
    public static final int TEST_BLOCK_SIZE = 64;

    private BlockDevice dev;
    private byte[] data;
    private SimpleAllocator alloc;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        init(2048);
    }

    private void init(final int dataSize) {
        data = new byte[dataSize];
        final ByteBufferDataStorage storage = new ByteBufferDataStorage(ByteBuffer.wrap(data));
        dev = new BlockDevice(TEST_BLOCK_SIZE, storage, storage, (alloc = new SimpleAllocator(dataSize/TEST_BLOCK_SIZE)));
    }

    public byte[] prepareBytes(final int cnt) {
        final byte[] out = new byte[cnt];
        for (int i = 0; i < cnt; i++) {
            out[i] = (byte) ((i + 1) % 0xFF);
        }
        log.info("prepared " + Arrays.toString(out));
        return out;
    }

    @Test
    public void testEmptyDataWriteAndRead() throws Exception {
        final int block = dev.openWriter().write(new byte[0]).close();
        final InputStream is = dev.openReader(block).asStream();
        assertEquals(-1, is.read());
    }

    @Test
    public void testSingleBlockWriteAndRead() throws Exception {
        final DataOutput writer = dev.openWriter();
        final byte[] bytes = prepareBytes(32);

        final int block = writer.write(bytes).close();

        final byte[] outBytes = IOUtils.readInputStreamToBytes(dev.openReader(block).asStream());
        log.info("in  bytes {}", Arrays.toString(bytes));
        log.info("out bytes {}", Arrays.toString(outBytes));

        assertTrue(Arrays.equals(bytes, outBytes));
    }

    @Test
    public void testMultiBlockWriteAndRead() throws Exception {
        final DataOutput writer = dev.openWriter();
        final byte[] bytes = prepareBytes(1025);

        final int block = writer.write(bytes).close();
        log.info("data after write : {}", Arrays.toString(data));

        final byte[] outBytes = IOUtils.readInputStreamToBytes(dev.openReader(block).asStream());
//
        assertTrue(Arrays.equals(bytes, outBytes));
    }

    @Test
    public void testLastWithNonAlignedData() throws Exception {
        final int dataSize = Block.calcUsefulPayload(TEST_BLOCK_SIZE) * 2 + 1;
        log.info("data size is " + dataSize);

        final byte[] bytes = prepareBytes(dataSize);
        final DataOutput writer = dev.openWriter();
        final int firstBlock = writer.write(bytes).close();
        assertEquals(0, firstBlock);

        assertEquals(2, dev.last(firstBlock).getNo());
    }

    @Test
    public void testLastWithAlignedData() throws Exception {
        final int dataSize = Block.calcUsefulPayload(TEST_BLOCK_SIZE) * 2;
        log.info("data size is " + dataSize);

        final byte[] bytes = prepareBytes(dataSize);
        final DataOutput writer = dev.openWriter();
        final int firstBlock = writer.write(bytes).close();
        assertEquals(0, firstBlock);

        assertEquals(1, dev.last(firstBlock).getNo());
    }

    @Test
    public void testAppendWithinBlock() throws Exception {
        final int dataSize = Block.calcUsefulPayload(TEST_BLOCK_SIZE) - 16;
        final byte[] inData = prepareBytes(dataSize);
        final int block = dev.openWriter().write(inData).close();

        final byte[] addData = {1, 2, 4, 5, 6};
        dev.openAppender(block).write(addData).close();
        final byte[] readData = IOUtils.readInputStreamToBytes(dev.openReader(block).asStream());
        assertEquals(addData.length + inData.length, readData.length);

        final byte[] totalDataWritten = new byte[readData.length];
        System.arraycopy(inData, 0, totalDataWritten, 0, inData.length);
        System.arraycopy(addData, 0, totalDataWritten, inData.length, addData.length);
        assertTrue(Arrays.equals(totalDataWritten, readData));
    }

    @Test
    public void testAppendExtBlock() throws Exception {
        final int dataSize = Block.calcUsefulPayload(TEST_BLOCK_SIZE) + 16;
        final byte[] inData = prepareBytes(dataSize);
        final int block = dev.openWriter().write(inData).close();

        final byte[] addData = prepareBytes(dataSize + 21);
        dev.openAppender(block).write(addData).close();
        final byte[] readData = IOUtils.readInputStreamToBytes(dev.openReader(block).asStream());
        assertEquals(addData.length + inData.length, readData.length);

        final byte[] totalDataWritten = new byte[readData.length];
        System.arraycopy(inData, 0, totalDataWritten, 0, inData.length);
        System.arraycopy(addData, 0, totalDataWritten, inData.length, addData.length);
        assertTrue(Arrays.equals(totalDataWritten, readData));
    }

    @Test
    public void testTouchedBlockProducesEmptyReadStream() throws Exception {
        this.init(128);
        final int blockNo = alloc.allocAnywhere(1);
        dev.touch(blockNo);
        log.info(Arrays.toString(data));
        final InputStream is = dev.openReader(blockNo).asStream();
        assertEquals(-1, is.read());
    }

}