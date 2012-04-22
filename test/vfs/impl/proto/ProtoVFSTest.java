package vfs.impl.proto;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.collections.Cf;
import util.io.IOUtils;
import vfs.api.VFile;
import vfs.api.VFileSystem;
import vfs.api.VFileSystemConfig;
import vfs.exception.VFileNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;

public class ProtoVFSTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(ProtoVFSTest.class);

    private static final int BLOCK_SIZE = 1024;
    public static final int CAPACITY = 10484760;

    VFileSystem vfs;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        vfs = initVFS();
    }

    protected ProtoVFS initVFS() {
        return new ProtoVFSFactory().create(
                ByteBuffer.wrap(new byte[CAPACITY]),
                new VFileSystemConfig(BLOCK_SIZE, false, true, '/')
        );
    }

    @Override
    public void tearDown() throws Exception {
        vfs.close();
        super.tearDown();
    }

    @Test
    public void testRootIsDirAndHaveNothingInEmptyFs() throws Exception {
        final VFile root = vfs.root();
        assertEquals("/", root.getName());
        assertEquals("/", root.getAbsoluteName());
        assertTrue(root.isDir());
        assertFalse(root.isFile());
        assertFalse(root.list().iterator().hasNext());
    }

    @Test
    public void testTwoRootsAreEqual() throws Exception {
        assertEquals(vfs.root(), vfs.root());
    }

    @Test
    public void testRootHasNullParent() throws Exception {
        assertNull(vfs.root().getParent());
    }

    @Test
    public void testAddDirToRootAndCanReadItBackToDir() throws Exception {
        final VFile root = vfs.root();
        log.debug("mkdir");
        final VFile tmp = vfs.fileManager().mkDir(root, "tmp");
        assertNotNull(tmp);
        assertEquals("/tmp", tmp.getAbsoluteName());
        assertTrue(tmp.isDir());
        assertEquals(Cf.list(tmp), Cf.list(root.list()));
        log.debug("reading child");
        assertEquals(tmp, root.child("tmp"));
        assertEquals("/tmp", tmp.getAbsoluteName());
    }

    @Test
    public void testAddFileToRootAndAllRelationsBetweenThemAreOk() throws Exception {
        final VFile root = vfs.root();
        final String newFileName = "HelloWorld.txt";
        final VFile file = vfs.fileManager().touch(root, newFileName);

        assertNotNull(file);

        assertEquals(newFileName, file.getName());
        assertEquals("/HelloWorld.txt", file.getAbsoluteName());

        final VFile parent = file.getParent();
        assertNotNull(parent);

        assertEquals(root, parent);
        assertEquals(file, root.child(newFileName));
        assertEquals(Cf.list(file), Cf.list(root.list()));

        assertEquals(file, vfs.fileManager().select("/HelloWorld.txt"));
    }

    @Test
    public void testAddFileToRootAndCanReadItBackToFile() throws Exception {
        final VFile root = vfs.root();
        final String newFileName = "HelloWorld.txt";
        final VFile file = vfs.fileManager().touch(root, newFileName);
        final PrintWriter pw = new PrintWriter(file.openOutput());
        final String line = "foobar barfoo";
        pw.print(line);
        pw.close();

        assertContainsOnly(file, line);

        final Iterator<VFile> children = root.list().iterator();
        assertContainsOnly(children.next(), line);
    }

    private void assertContainsOnly(final VFile file, final String line) throws IOException {
        final String firstLine = doReadLine(file);
        assertEquals(line, firstLine);
    }

    private String doReadLine(final VFile file) throws IOException {
        final InputStream input = file.openInput();
        final byte[] bytes = IOUtils.readInputStreamToBytes(input);
        final Scanner s = new Scanner(new ByteArrayInputStream(bytes));
        final String firstLine = s.nextLine().trim();
        s.close();
        return firstLine;
    }

    @Test
    public void testCannotAddFileToFileAsChild() throws Exception {
        final VFile foo = vfs.fileManager().touch(vfs.root(), "foo");
        assertNotNull(foo);
        assertTrue(foo.isFile());
        try {
            final VFile bar = vfs.fileManager().touch(foo, "bar");
            fail();
        } catch (IllegalArgumentException e) {
            //ok
        }
    }

    @Test
    public void testAddMultiLevelDirsWithFilesToRootAndCanReadItBackOk() throws Exception {
        final VFile root = vfs.root();
        final VFile topDir = vfs.fileManager().mkDir(root, "01");
        final VFile midDir = vfs.fileManager().mkDir(topDir, "01");
        final VFile lowDir1 = vfs.fileManager().mkDir(midDir, "01");
        final VFile lowDir2 = vfs.fileManager().mkDir(midDir, "02");
        assertEquals(Cf.list(topDir), Cf.list(root.list()));
        assertEquals(Cf.list(midDir), Cf.list(topDir.list()));
        assertEquals(Cf.list(lowDir1, lowDir2), Cf.list(midDir.list()));
        assertEquals(Collections.emptyList(), Cf.list(lowDir1.list()));
        assertEquals(Collections.emptyList(), Cf.list(lowDir2.list()));

        final PrintWriter pw1 = new PrintWriter(vfs.fileManager().touch(lowDir1, "tmp.txt").openOutput());
        pw1.println("foo");
        pw1.close();

        final PrintWriter pw2 = new PrintWriter(vfs.fileManager().touch(lowDir2, "tmp.txt").openOutput());
        pw2.println("bar");
        pw2.close();

        assertEquals("foo", doReadLine(vfs.fileManager().select("/01/01/01/tmp.txt")));
        assertEquals("bar", doReadLine(vfs.fileManager().select("/01/01/02/tmp.txt")));

    }

    @Test
    public void testMkdirsWithOkPathProducesOkDir() throws Exception {
        final String path = "/tmp/foo/bar/42/tmp";
        final VFile newDir = vfs.fileManager().mkDirs(path);
        assertNotNull(newDir);
        assertEquals(path, newDir.getAbsoluteName());
        assertEquals(newDir, vfs.root().child("tmp").child("foo").child("bar").child("42").child("tmp"));
    }

    @Test
    public void testMkdirsWithNonOkPathProducesNull() throws Exception {
        final VFile newFile = vfs.fileManager().touch(vfs.fileManager().mkDirs("/tmp/"), "foo");
        assertNotNull(newFile);
        assertTrue(newFile.isFile());
        assertNull(vfs.fileManager().mkDirs("/tmp/foo/bar"));
    }

    @Test
    public void testCannotDeleteRoot() throws Exception {
        assertFalse(vfs.fileManager().rm(vfs.root()));
    }

    public void testCannotDeleteNonEmptyDir() throws Exception {
        vfs.fileManager().mkDirs("/tmp/foo/bar");
        assertFalse(vfs.fileManager().rm(vfs.fileManager().select("/tmp/foo")));
    }

    @Test
    public void testDirHasNoDeletedChildren() throws Exception {
        final VFile root = vfs.root();
        final VFile foo = vfs.fileManager().touch(root, "foo");
        final VFile bar = vfs.fileManager().touch(root, "bar");
        final VFile baz = vfs.fileManager().touch(root, "baz");
        final VFile zag = vfs.fileManager().mkDir(root, "zag");
        final VFile zap = vfs.fileManager().mkDir(root, "zap");

        assertTrue(vfs.fileManager().rm(bar));

        assertEquals(Cf.set(foo, baz, zag, zap), Cf.set(root.list()));
    }

    @Test
    public void testDeletedDirAndFileCanBeRecreated() throws Exception {
        final VFile dir = vfs.fileManager().mkDirs("/tmp/foo/bar/baz");
        VFile file = vfs.fileManager().touch(dir, "file");
        assertTrue(vfs.fileManager().rm(file));
        assertEquals(Collections.emptyList(), Cf.list(dir.list()));
        file = vfs.fileManager().touch(dir, "file");
        assertEquals(Cf.list(file), Cf.list(dir.list()));

        final VFile parent = dir.getParent();
        assertTrue(vfs.fileManager().rm(file));
        assertTrue(vfs.fileManager().rm(dir));
        assertEquals(Collections.emptyList(), Cf.list(parent.list()));
        final VFile recreatedDir = vfs.fileManager().mkDir(parent, "baz");
        assertEquals(Cf.list(recreatedDir), Cf.list(parent.list()));
    }

    @Test
    public void testRefToDeletedVFileThrowsUpOnSideEffectiveMethods() throws Exception {
        final VFile dir = vfs.fileManager().mkDirs("/tmp/foo/bar");
        final VFile file = vfs.fileManager().touch(dir, "boo");

        assertTrue(vfs.fileManager().rm(file));

        try {
            file.openInput();
            fail();
        } catch (VFileNotFoundException e) {
            //ok
        }

        try {
            file.openOutput();
            fail();
        } catch (VFileNotFoundException e) {
            //ok
        }

        assertTrue(vfs.fileManager().rm(dir));

        try {
            dir.list();
            fail();
        } catch (VFileNotFoundException e) {
            //ok
        }

        try {
            vfs.fileManager().touch(dir, "foo");
            fail();
        } catch (VFileNotFoundException e) {
            //ok
        }

        try {
            vfs.fileManager().mkDir(dir, "foo");
            fail();
        } catch (VFileNotFoundException e) {
            //ok
        }

    }

    @Test
    public void testCantCreateFilesWithSpecialNames() throws Exception {
        testFailCreationAndTouchOnName(".");
        testFailCreationAndTouchOnName(null);
        testFailCreationAndTouchOnName("..");
        testFailCreationAndTouchOnName(" . ");
        testFailCreationAndTouchOnName("/");
        testFailCreationAndTouchOnName("/.");
        testFailCreationAndTouchOnName("////");
        testFailCreationAndTouchOnName("./");
        testFailCreationAndTouchOnName("../.");
        testFailCreationAndTouchOnName("./..");
        testFailCreationAndTouchOnName("dodoo/dodooo/dooodo");
    }

    private void testFailCreationAndTouchOnName(final String name) {
        try {
            vfs.fileManager().touch(vfs.root(), name);
            fail();
        } catch (RuntimeException e) {
            //ok
        }
        try {
            vfs.fileManager().mkDir(vfs.root(), name);
            fail();
        } catch (RuntimeException e) {
            //ok
        }
    }
}
