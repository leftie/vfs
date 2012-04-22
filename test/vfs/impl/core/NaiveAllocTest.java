package vfs.impl.core;

import junit.framework.TestCase;

import java.util.BitSet;

public class NaiveAllocTest extends TestCase {

    public void testConsecutiveAlloc() throws Exception {
        final NaiveAlloc a = new NaiveAlloc(5);
        for (int i = 0; i < 5; i ++) {
            assertEquals(i, a.allocAnywhere(1));
        }
    }

    public void testConsecutiveAllocAndFree() throws Exception {
        final BitSet src = new BitSet(6);
        src.set(1);
        src.set(3);
        src.set(5);
        final NaiveAlloc a = new NaiveAlloc(src);
        assertEquals(2, a.allocAnywhere(1));
        assertEquals(4, a.allocAnywhere(1));
        a.free(2);
        a.free(5);
        a.allocAnywhere(1);
        a.allocAnywhere(1);
    }

}
