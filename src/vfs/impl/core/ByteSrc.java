package vfs.impl.core;

public interface ByteSrc {

    byte[] read(long from, int length);

    void close();
}
