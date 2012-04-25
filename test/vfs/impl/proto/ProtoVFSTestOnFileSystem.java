package vfs.impl.proto;

import org.junit.Test;
import vfs.api.VFile;
import vfs.api.VFileSystemConfig;

import java.io.File;
import java.io.OutputStream;

public class ProtoVFSTestOnFileSystem extends ProtoVFSTest {
    @Override
    protected ProtoVFS initVFS() {
        return new ProtoVFSFactory().create(new File("/tmp/_test.vfs"), true, new VFileSystemConfig(1024, true, true, '/'));
    }


    @Test
    public  void testPerformance() throws Exception {
        final VFile dir = vfs.fileManager().mkDirs("/tmp/foo/bar");
        assertNotNull(dir);

        final VFile file = vfs.fileManager().touch(dir, "boo");
        assertNotNull(file);

        OutputStream output = file.openOutput();

        try {
            byte[] oneK = new byte[1024];
            for (int i = 0; i < oneK.length; i++) oneK[i] = (byte) i;


            for (int i = 0; i < 1000 * 1000; i++) {
                output.write(oneK);
            }
        } finally {
            output.close();
        }


    }
}
