package vfs.api;

import vfs.exception.VFSException;

public interface VFileSystem {

    VFile getRoot();

    VFileManager fileManager();

    void close() throws VFSException;
}
