package vfs.impl.proto;

import vfs.api.VFileSystemConfig;
import vfs.api.VFileSystemFactory;
import vfs.exception.VFSCorruptException;
import vfs.exception.VFSException;
import vfs.impl.core.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class ProtoVFSFactory implements VFileSystemFactory {

    @Override
    public ProtoVFS open(final File target, final VFileSystemConfig cfg) throws VFSCorruptException, VFSException {
        final RandomAccessFile file;
        try {
            file = new RandomAccessFile(target, "rw");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final RandomAccessFileDataStorage storage = new RandomAccessFileDataStorage(file);
        final BitSet bitset = storage.loadOccupanceBitMap(cfg.getBlockSize());
        final BlockAllocator alloc = new SimpleAllocator(bitset);
        final BlockDevice device = new BlockDevice(cfg.getBlockSize(), storage, storage, alloc);
        final ProtoVFS vfs = new ProtoVFS(device, alloc, cfg);
        if (vfs.getRoot() == null) {
            throw new VFSCorruptException();
        }
        return vfs;
    }

    @Override
    public ProtoVFS create(final ByteBuffer bb, final VFileSystemConfig cfg) {

        final ByteBufferDataStorage storage = new ByteBufferDataStorage(bb);
        final BlockAllocator alloc = new SimpleAllocator(Integer.MAX_VALUE / cfg.getBlockSize());
        final BlockDevice device = new BlockDevice(cfg.getBlockSize(), storage, storage, alloc);

        final ProtoVFS vfs = new ProtoVFS(device, alloc, cfg);
        vfs.writeRoot(0);
        return vfs;
    }

    @Override
    public ProtoVFS create(final File target, final boolean overwrite, final VFileSystemConfig cfg) {
        if (target.exists()) {
            if (!overwrite) {
                throw new RuntimeException(target + " already exists");
            } else {
                if (!target.isFile()) {
                    throw new RuntimeException(target + " is not a file");
                }
            }
        }
        final File parent = target.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("could not assure existance of directory " + parent + " to host a VFS file " + target.getName());
        }

        final RandomAccessFileDataStorage bbStuff;
        try {
            bbStuff = new RandomAccessFileDataStorage(new RandomAccessFile(target, "rw"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        final BlockAllocator alloc = new SimpleAllocator(Integer.MAX_VALUE / cfg.getBlockSize());
        final BlockDevice device = new BlockDevice(cfg.getBlockSize(), bbStuff, bbStuff, alloc);

        final ProtoVFS vfs = new ProtoVFS(device, alloc, cfg);
        vfs.writeRoot(0);
        return vfs;
    }


}
