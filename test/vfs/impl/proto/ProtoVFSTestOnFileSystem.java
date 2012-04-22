package vfs.impl.proto;

import vfs.api.VFileSystemConfig;

import java.io.File;

public class ProtoVFSTestOnFileSystem extends ProtoVFSTest {
    @Override
    protected ProtoVFS initVFS() {
        return new ProtoVFSFactory().create(new File("/tmp/_test.vfs"), true, new VFileSystemConfig(1024, true, true, '/'));
    }

}
