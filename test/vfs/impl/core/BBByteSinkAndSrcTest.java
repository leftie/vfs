package vfs.impl.core;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BBByteSinkAndSrcTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(BBByteSinkAndSrcTest.class);

    BBByteSinkAndSrc bb;
    byte[] inner;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        inner = new byte[64];
        bb = new BBByteSinkAndSrc(ByteBuffer.wrap(inner));
    }

    public void testReadAndWrite() throws Exception {
        final OutputStream out = bb.openOut(2);
        out.write(new byte[]{0, 1, 2, 3});
        out.close();
        log.info(Arrays.toString(inner));

        assertTrue(Arrays.equals(new byte[]{0, 0, 0, 1, 2, 3, 0, 0}, bb.read(0, 8)));
    }
}
