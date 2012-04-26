package vfs.impl.core;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class BlockTest extends TestCase {

    private static final byte[] PAYLOAD = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, (byte) 0xee, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
    public static final int BLOCK_SIZE = 128;

    @Test
    public void testEmpty() throws Exception {
        encodeDecodeAndCompare(new Block(42, 21, new byte[]{}), new byte[]{});
    }

    @Test
    public void testSimple() throws Exception {
        encodeDecodeAndCompare(new Block(42, 21, PAYLOAD), PAYLOAD);
    }

    @Test
    public void testThrowOnBad() throws Exception {
        final Block b = new Block(42, 21, PAYLOAD);
        try {
            Block.encode(new ByteArrayOutputStream(), b, 22);
            throw new AssertionError();
        } catch (IllegalArgumentException e) {
            //ok
        }
    }

    @Test
    public void testEmptyArrayDecodesToEmptyBlock() throws Exception {
        final byte[] bytes = new byte[32];
        final Block block = Block.decode(new ByteArrayInputStream(bytes), 32);
        assertEquals(0, block.getNo());
        assertEquals(0, block.getNext());
        assertEquals(0, block.getData().length);
    }

    private static Block encodeDecodeAndCompare(final Block orig, final byte[] origPayload) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(BLOCK_SIZE);
        Block.encode(baos, orig, BLOCK_SIZE);
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final Block decoded = Block.decode(bais, BLOCK_SIZE);
        assertEquals(orig, decoded);
        assertTrue(Arrays.equals(origPayload, decoded.getData()));
        return decoded;
    }

    @Test
    public void testLargeBlockNos() throws Exception {
        for (int i = 0; i < 102400; i++) {
            Block block = new Block(i, i + 1, PAYLOAD);
            assertEquals(i, block.getNo());
            assertEquals(i+1, block.getNext());
            assertEquals(block , encodeDecodeAndCompare(block, PAYLOAD));
        }
    }
}
