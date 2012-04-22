package vfs.impl.core;

public interface BlockAllocator {

    int allocAnywhere(final int num);

    int allocNextTo(final int blockNo);

    void free(final int block);

    void free(final int startBlock, final int num);

    boolean isFree(final int block);
}
