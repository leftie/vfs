package vfs.impl;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.collections.Cf;
import vfs.api.VFile;
import vfs.api.VFileSystem;
import vfs.api.VFileSystemConfig;
import vfs.impl.proto.ProtoVFSFactory;

import java.io.File;

public class VFSTool extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(VFSTool.class);

    @Test
    public void testListTmpFile() throws Exception {
        final VFileSystem writeVFS = new ProtoVFSFactory().create(new File("/tmp/_test.vfs"), true, new VFileSystemConfig(64, true, true, '/'));

        final VFile first = writeVFS.fileManager().mkDir(writeVFS.getRoot(), "01");
        final VFile fFile = writeVFS.fileManager().touch(first, "foo.txt");
        final VFile second = writeVFS.fileManager().mkDir(writeVFS.getRoot(), "02");
        final VFile sFile = writeVFS.fileManager().touch(second, "bar.txt");
        final VFile third = writeVFS.fileManager().mkDir(writeVFS.getRoot(), "03");
        final VFile tFile = writeVFS.fileManager().touch(third, "baz.txt");

        System.out.println(Cf.list(first.list()));
        System.out.println(Cf.list(second.list()));
        System.out.println(Cf.list(third.list()));

        writeVFS.close();

        final VFileSystem readVfs = new ProtoVFSFactory().open(new File("/tmp/_test.vfs"), new VFileSystemConfig(64, true, true, '/'));
        doPrint(readVfs.getRoot());
        readVfs.close();
    }

    void doPrint(final VFile file) {
        log.info(file.getAbsolutePath());
        if (file.isDir()) {
            for (final VFile child : file.list()) {
                doPrint(child);
            }
        }
    }
}
