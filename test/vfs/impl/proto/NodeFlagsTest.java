package vfs.impl.proto;

import junit.framework.TestCase;

public class NodeFlagsTest extends TestCase {

    public void testIsZip() throws Exception {
        assertTrue(new NodeFlags(false, true).isZipped());
        assertFalse(new NodeFlags(false, false).isZipped());
    }

    public void testIsDir() throws Exception {
        assertTrue(new NodeFlags(true, false).isDir());
        assertFalse(new NodeFlags(true, false).isFile());
        assertFalse(new NodeFlags(false, false).isDir());
        assertTrue(new NodeFlags(false, false).isFile());
    }
}
