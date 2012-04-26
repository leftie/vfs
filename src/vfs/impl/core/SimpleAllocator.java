package vfs.impl.core;

import net.jcip.annotations.NotThreadSafe;

import java.util.BitSet;

@NotThreadSafe
public class SimpleAllocator implements BlockAllocator {

    private final BitSet bs;

    private int next;

    public SimpleAllocator(final BitSet src) {
        this.bs = src.get(0, src.length());
        this.next = src.nextSetBit(1) + 1;
    }

    public SimpleAllocator(final int blockCnt) {
        this.bs = new BitSet(blockCnt);
        this.next = 0;
    }

    @Override
    public int allocAnywhere(final int blockNum) {
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
        next = Math.min(next, startBlock);
    }


    @Override
    public boolean isFree(final int block) {
        return block > 0 && !bs.get(block);
    }

    private int doAlloc(final int pos) {
        if (bs.get(pos)) {
            throw new AssertionError("already allocated " + pos);
        }
        bs.set(pos);
        next = findNext(pos + 1);
        return pos;
    }

    private int findNext(final int startingFrom) {
        return bs.nextClearBit(startingFrom);
    }
}
