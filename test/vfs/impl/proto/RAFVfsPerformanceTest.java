package vfs.impl.proto;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vfs.api.VFile;
import vfs.api.VFileSystemConfig;

import java.io.File;
import java.io.OutputStream;

public class RAFVfsPerformanceTest extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(RAFVfsPerformanceTest.class);

    @Test
    public void testPerformance() throws Exception {

        final ProtoVFS vfs = new ProtoVFSFactory().create(new File("/tmp/_perf_test.vfs"), true, new VFileSystemConfig(1024, true, true, '/'));

        final VFile dir = vfs.fileManager().mkDirs("/tmp/foo/bar");
        assertNotNull(dir);

        final VFile file = vfs.fileManager().touch(dir, "boo");
        assertNotNull(file);

        final long startTs = System.currentTimeMillis();
        OutputStream output = file.openFileOutput();

        try {
            byte[] oneK = new byte[1024];
            for (int i = 0; i < oneK.length; i++) oneK[i] = (byte) i;

            for (int i = 0; i < 1000 * 100; i++) {
                output.write(oneK);
            }
        } finally {
            output.close();
        }

        log.info("done in " + (System.currentTimeMillis() - startTs));
    }

}
