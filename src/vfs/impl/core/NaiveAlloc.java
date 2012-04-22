package vfs.impl.core;

import net.jcip.annotations.NotThreadSafe;

import java.util.BitSet;

@NotThreadSafe
public class NaiveAlloc implements BlockAllocator {

    private final BitSet bs;

    private int next;

    public NaiveAlloc(final BitSet bs) {
        this.bs = bs.get(0, bs.length());
        this.next = bs.nextSetBit(1) + 1;
    }

    public NaiveAlloc(final int cnt) {
        this.bs = new BitSet(cnt);
        this.next = 0;
    }

    @Override
    public int allocAnywhere(final int num) {
        return doAlloc(next);
    }

    @Override
    public int allocNextTo(final int blockNo) {
        return allocAnywhere(1);
    }

    @Override
    public void free(final int block) {
        this.free(block, 1);
    }

    @Override
    public void free(final int startBlock, final int num) {
        bs.clear(startBlock, startBlock + num);
        next = startBlock;
    }

    @Override
    public boolean isFree(final int block) {
        return block > 0 && !bs.get(block);
    }

    private int doAlloc(final int pos) {
        if (pos >= bs.size()) {
            final int possibleNextFree = bs.nextClearBit(1);
        }
        if (bs.get(pos)) {
            throw new AssertionError("already allocated " + pos);
        }
        bs.set(pos);
        next = bs.nextClearBit(pos);
        return pos;
    }
}
