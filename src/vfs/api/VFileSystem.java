package vfs.api;

import vfs.exception.VFSException;

public interface VFileSystem {

    VFile root();

    VFileManager fileManager();

    void close() throws VFSException;
}
