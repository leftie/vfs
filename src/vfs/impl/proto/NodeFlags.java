package vfs.impl.proto;

final class NodeFlags {

    private static final int IS_DIR = 1 << 31; //else file
    private static final int IS_ZIP = 1 << 30;

    private final int value;

    NodeFlags(final boolean isDir, final boolean isZip) {
        int value = 0;
        if (isDir) {
            value |= IS_DIR;
        }
        if (isZip) {
            value |= IS_ZIP;
        }

        this.value = value;
    }

    NodeFlags(final int value) {
        this.value = value;
    }

    boolean isDir() {
        return (value & IS_DIR) != 0;
    }

    boolean isFile() {
        return !isDir();
    }

    boolean isZipped() {
        return (value & IS_ZIP) != 0;
    }

    int asIntValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) { //generated
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NodeFlags flags = (NodeFlags) o;

        if (value != flags.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "NodeFlags{" +
                Long.toHexString(value) +
                '}';
    }
}
