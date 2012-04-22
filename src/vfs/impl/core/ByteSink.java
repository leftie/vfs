package vfs.impl.core;

import java.io.OutputStream;

public interface ByteSink {

    void flush();

    void close();

    OutputStream openOut(final long pos);

}
