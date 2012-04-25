package vfs.api;

public class VFileSystemConfig {

    private static final int MIN_BLOCK_SIZE = 64;

    private final int blockSize;
    private final boolean doCompress;
    private final boolean doChecksum;
    private final char separatorChar;
    private final String separator;

    public VFileSystemConfig(final int blockSize, final boolean doCompress, final boolean doChecksum, final char separatorChar) {
        if (blockSize < MIN_BLOCK_SIZE) {
            throw new RuntimeException("block size too small. min allowed block size is " + MIN_BLOCK_SIZE);
        }
        this.blockSize = blockSize;
        this.doCompress = doCompress;
        this.doChecksum = doChecksum;
        this.separatorChar = separatorChar;
        this.separator = String.valueOf(separatorChar);
    }

    public int getBlockSize() {
        return blockSize;
    }

    public boolean isDoCompress() {
        return doCompress;
    }

    public boolean isDoChecksum() {
        return doChecksum;
    }

    public char getSeparatorChar() {
        return separatorChar;
    }

    public String getSeparator() {
        return separator;
    }
}
