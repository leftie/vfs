package vfs.api;

import vfs.exception.VFSCorruptException;
import vfs.exception.VFSException;

import java.io.File;
import java.nio.ByteBuffer;

public interface VFileSystemFactory {

    VFileSystem create(final ByteBuffer bb, final VFileSystemConfig cfg);

    VFileSystem create(final File target, final boolean overwrite, final VFileSystemConfig cfg);

    VFileSystem open(final File target, final VFileSystemConfig cfg) throws VFSCorruptException, VFSException;

}
