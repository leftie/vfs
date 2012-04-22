package vfs.impl.proto;

import com.google.protobuf.ByteString;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.collections.Cf;
import vfs.api.VFile;
import vfs.api.VFileSystem;
import vfs.api.VFileSystemConfig;
import vfs.exception.VFSException;
import vfs.exception.VFileNotFoundException;
import vfs.impl.core.BlockAllocator;
import vfs.impl.core.BlockDevice;
import vfs.impl.core.BlockReader;
import vfs.impl.core.BlockWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.StringTokenizer;

class ProtoVFS implements VFileSystem {

    private static final Logger log = LoggerFactory.getLogger(ProtoVFS.class);

    private static final int ROOT_BLOCK_NO = 0;

    private final BlockDevice device;
    private final BlockAllocator allocator;
    private final ProtoFileManager fm;
    private final VFileSystemConfig cfg;

    private final String rootName;

    ProtoVFS(final BlockDevice device, final BlockAllocator allocator, final VFileSystemConfig cfg) {
        this.device = device;
        this.allocator = allocator;
        this.cfg = cfg;
        this.rootName = cfg.getSeparator();
        //noinspection ThisEscapedInObjectConstruction
        this.fm = new ProtoFileManager(this); //this escapes only locally to trusted code. so don't bother.
    }

    void writeRoot(final long ts) {
        final int rootNo = allocator.allocAnywhere(1);
        final int dataNo = allocator.allocNextTo(rootNo);
        assert rootNo == 0;
        final BlockWriter writer = device.openWriter(rootNo);
        try {
            final VFS.Node node = VFS.Node.newBuilder()
                    .setNo(ROOT_BLOCK_NO)
                    .setName(rootName)
                    .setDataBlockNo(dataNo)
                    .setParentNo(-1)
                    .setFlags(new NodeFlags(true, cfg.isDoCompress()).asIntValue())
                    .setTimestamp(ts)
                    .build();
            try {
                node.writeDelimitedTo(writer.asStream());
                device.touch(dataNo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            writer.close();
        }
    }

    private ProtoVFile resolve(final int fileNo, final ProtoVFile parent) {
        return doReadFileFromDevice(fileNo, parent);
    }

    @Override
    public ProtoVFile root() {
        final InputStream is = device.openReader(ROOT_BLOCK_NO).asStream();
        try {
            final VFS.Node rootNode = VFS.Node.parseDelimitedFrom(is);
            return buildFile(rootNode, null);
        } catch (IOException e) {
            throw new VFSException(e);
        }
    }

    @Override
    public void close() throws VFSException {
        device.close();
    }

    Iterable<ProtoVFile> list(final ProtoVFile file) {
        if (!file.isDir()) {
            throw new IllegalArgumentException("is not a dir :" + file);
        }
        final BlockReader reader = device.openReader(file.getProtoNode().getDataBlockNo());
        log.debug("data block for {} is {}", file, file.getProtoNode().getDataBlockNo());
        try {
            final InputStream input = reader.asStream();
            final List<ProtoVFile> out = Cf.newLinkedList();
            final VFS.DirEntry.Builder nextEntry = VFS.DirEntry.newBuilder();
            while (nextEntry.mergeDelimitedFrom(input)) {
                final VFS.DirEntry readEntry = nextEntry.build();
                log.debug("read entry {}", readEntry);
                out.add(getFile(readEntry, file));
                nextEntry.clear();
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            reader.close();
        }
    }

    private ProtoVFile getFile(final VFS.DirEntry entry, final ProtoVFile parent) {
        return doReadFileFromDevice(entry.getInode(), parent);
    }

    @Nullable
    ProtoVFile getParent(final ProtoVFile protoVFile) {
        log.debug("getParent({})", protoVFile);
        final int parentNo = protoVFile.getProtoNode().getParentNo();
        if (parentNo == -1) {
            return null;
        }
        final String abs = protoVFile.getAbsoluteName();
        final String parentPath = abs.substring(0, abs.lastIndexOf(cfg.getSeparatorChar()));
        final ProtoVFile parent = select(parentPath);
        if (parent == null) {
            throw new VFileNotFoundException(parentPath);
        }
        if (!parent.isDir()) {
            throw new AssertionError("not a dir : " + parent);
        }

        return parent;
    }

    @Override
    public ProtoFileManager fileManager() {
        return fm;
    }

    InputStream openInput(final ProtoVFile file) {
        assertIsFile(file);
        final int fileNo = file.getProtoNode().getNo();
        if (allocator.isFree(fileNo)) {
            throw new VFileNotFoundException("not found: " + file.toString());
        }
        final BlockReader metaReader = device.openReader(fileNo);
        final VFS.Node metaNode = doReadNodeFrom(metaReader);
        final int dataBlockNo = metaNode.getDataBlockNo();
        log.debug("data block for {} is {}", file.getAbsoluteName(), dataBlockNo);
        try {
            final BlockReader dataReader = device.openReader(dataBlockNo);
            try {
                return new InputStream() {
                    final InputStream delegate = dataReader.asStream();

                    @Override
                    public int read() throws IOException {
                        return delegate.read();
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                        dataReader.close();
                    }
                };
            } finally {
                dataReader.close();
            }
        } finally {
            metaReader.close();
        }
    }

    OutputStream openOutput(final ProtoVFile file) {
        assertIsFile(file);
        if (allocator.isFree(file.getProtoNode().getNo())) {
            throw new VFileNotFoundException("not found: " + file.toString());
        }

        return device.openWriter(file.getProtoNode().getDataBlockNo()).asStream();
    }

    private void assertIsFile(final ProtoVFile file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("is not a file :" + file);
        }
    }

    ProtoVFile doReadFileFromDevice(final int nodeNo, final ProtoVFile parent) {
        log.debug("reading node {}", nodeNo);
        if (allocator.isFree(nodeNo)) {
            throw new RuntimeException("node is free " + nodeNo);
        }
        synchronized (device) {

            final BlockReader reader = device.openReader(nodeNo);
            try {
                final InputStream is = reader.asStream();
                try {
                    final VFS.Node node = VFS.Node.parseDelimitedFrom(is);
                    assert node != null;
                    assert nodeNo == node.getNo();

                    return buildFile(node, parent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                reader.close();
            }
        }
    }

    private ProtoVFile buildFile(final VFS.Node node, @Nullable final ProtoVFile parent) {
        return new ProtoVFile(
                this,
                node.getName(),
                buildAbsolutName(parent, node.getName()),
                new NodeFlags(node.getFlags()),
                node
        );
    }

    ProtoVFile touch(final VFile parentDir, final String fileName) throws VFileNotFoundException, VFSException {
        return touch(resolve(parentDir), fileName);
    }

    private ProtoVFile touch(final ProtoVFile parentDir, final String newFileName) throws VFileNotFoundException, VFSException {
        if (!parentDir.isDir()) {
            throw new IllegalArgumentException(parentDir + " is not a dir");
        }

        final int newFileNo = allocator.allocAnywhere(1);
        final int dataBlockNo = allocator.allocNextTo(newFileNo);
        log.debug("allocated {} for header and {} for data", newFileNo, dataBlockNo);

        final int parentNo = parentDir.getProtoNode().getNo();
        if (log.isInfoEnabled()) {
            log.debug("parent fileNo is {}, new file fileNo is {}", parentNo, newFileNo);
        }

        assert newFileNo != parentNo;

        final VFS.Node newNode = VFS.Node.newBuilder()
                .setFlags(new NodeFlags(false, cfg.isDoCompress()).asIntValue())
                .setNo(newFileNo)
                .setParentNo(parentDir.getProtoNode().getNo())
                .setName(newFileName).setName(newFileName)
                .setDataBlockNo(dataBlockNo)
                .setTimestamp(System.currentTimeMillis())
                .setChecksum(ByteString.EMPTY)
                .setSize(0).build();

        log.debug("writing data-node " + newNode + " to " + newFileNo);
        final BlockWriter writer = device.openWriter(newFileNo);
        try {
            writeNodeTo(newNode, writer);
            device.touch(dataBlockNo);
        } finally {
            writer.close();
        }
        final int parendDataNodeNo = parentDir.getProtoNode().getDataBlockNo();
        log.debug("writing parent dir-entry to " + parendDataNodeNo);
        final BlockWriter toParentAppender = device.openAppender(parendDataNodeNo);
        try {
            final VFS.DirEntry newDirEntry = VFS.DirEntry.newBuilder().setInode(newFileNo).setName(newFileName).build();
            newDirEntry.writeDelimitedTo(toParentAppender.asStream());
        } catch (IOException e) {
            throw new VFSException(e);
        } finally {
            toParentAppender.close();
        }
        return resolve(newFileNo, parentDir);
    }

    ProtoVFile resolve(String path) throws VFileNotFoundException {
        path = normalize(path);
        final StringTokenizer tkz = new StringTokenizer(path, cfg.getSeparator());
        ProtoVFile current = this.root();
        while (tkz.hasMoreTokens()) {
            final String nameToken = tkz.nextToken();
            current = current.child(nameToken);
            if (current == null) {
                throw new VFileNotFoundException("not found :" + path);
            }
        }
        return current;
    }

    //copy-paste and code-nice from java.io.File
    private String normalize(final String pathname, final int len, final int off) {
        final char sep = cfg.getSeparatorChar();
        if (len == 0) {
            return pathname;
        }
        int n = len;
        while ((n > 0) && (pathname.charAt(n - 1) == sep)) {
            n--;
        }
        if (n == 0) {
            return cfg.getSeparator();
        }
        final StringBuilder sb = new StringBuilder(pathname.length());
        if (off > 0) {
            sb.append(pathname.substring(0, off));
        }
        char prevChar = 0;
        for (int i = off; i < n; i++) {
            final char c = pathname.charAt(i);
            if ((prevChar == sep) && (c == sep)) {
                continue;
            }
            sb.append(c);
            prevChar = c;
        }
        return sb.toString();
    }

    //copy-paste and code-nice from java.io.File
    String normalize(final String pathname) {
        final char sep = cfg.getSeparatorChar();
        final int n = pathname.length();
        char prevChar = 0;
        for (int i = 0; i < n; i++) {
            final char c = pathname.charAt(i);
            if ((prevChar == sep) && (c == sep)) {
                return normalize(pathname, n, i - 1);
            }
            prevChar = c;
        }
        if (prevChar == sep) {
            return normalize(pathname, n, n - 1);
        }
        return pathname;
    }

    VFS.Node doReadNodeFrom(final BlockReader reader) throws VFSException {
        try {
            return VFS.Node.parseDelimitedFrom(reader.asStream());
        } catch (IOException e) {
            throw new VFSException(e);
        }
    }

    BlockWriter writeNodeTo(final VFS.Node node, final BlockWriter writer) throws VFSException {
        try {
            node.writeDelimitedTo(writer.asStream());
            return writer;
        } catch (IOException e) {
            throw new VFSException(e);
        }
    }


    BlockWriter writeDirEntryTo(final VFS.DirEntry entry, final BlockWriter writer) throws VFSException {
        try {
            entry.writeDelimitedTo(writer.asStream());
            return writer;
        } catch (IOException e) {
            throw new VFSException(e);
        }
    }

    void writeDirEntryTo(final VFS.DirEntry entry, final OutputStream stream) throws VFSException {
        try {
            entry.writeDelimitedTo(stream);
        } catch (IOException e) {
            throw new VFSException(e);
        }
    }


    private ProtoVFile mkDir(final VFile parentDir, final String dirName) {
        return mkDir(resolve(parentDir), dirName);
    }

    ProtoVFile mkDir(final ProtoVFile parentDir, final String dirName) {
        log.debug("mkdir ({}),({})", parentDir.getAbsoluteName(), dirName);
        if (!parentDir.isDir()) {
            throw new IllegalArgumentException(parentDir + " is not a dir");
        }

        final int blockForNewHead = allocator.allocAnywhere(1);
        final int blockForNewDirEntries = allocator.allocAnywhere(1);
        log.debug("head is {}, data for dir is {}", blockForNewHead, blockForNewDirEntries);
        final BlockWriter dirWriter = device.openWriter(blockForNewHead);
        final NodeFlags flags = new NodeFlags(true, false);
        final VFS.Node dirNode = VFS.Node.newBuilder()
                .setName(dirName)
                .setDataBlockNo(blockForNewDirEntries)
                .setParentNo(parentDir.getProtoNode().getNo())
                .setFlags(flags.asIntValue())
                .setNo(blockForNewHead)
                .setTimestamp(System.currentTimeMillis()).build();
        try {
            writeNodeTo(dirNode, dirWriter);
        } finally {
            dirWriter.close();
        }

        final BlockWriter parentAppender = device.openAppender(parentDir.getProtoNode().getDataBlockNo());
        try {
            final VFS.DirEntry dirEntry = VFS.DirEntry.newBuilder()
                    .setInode(blockForNewHead)
                    .setName(dirName)
                    .build();

            writeDirEntryTo(dirEntry, parentAppender);
        } finally {
            parentAppender.close();
        }
        device.touch(blockForNewDirEntries);
        return new ProtoVFile(this, dirName, buildAbsolutName(parentDir, dirName), flags, dirNode);
    }

    private String buildAbsolutName(@Nullable final ProtoVFile parentDir, final String childName) {
        if (parentDir == null) {
            return normalize(cfg.getSeparator() + childName);
        }
        String newAbsName = parentDir.getAbsoluteName();
        if (newAbsName.charAt(newAbsName.length() - 1) != cfg.getSeparatorChar()) {
            newAbsName = newAbsName + cfg.getSeparator() + childName;
        } else {
            newAbsName += childName;
        }
        return newAbsName;
    }


    @Nullable
    ProtoVFile select(final String fullPath) {
        return resolve(fullPath);
    }

    public ProtoVFile mkDirs(String fullPath) {
        fullPath = normalize(fullPath);
        final StringTokenizer tkz = new StringTokenizer(fullPath, cfg.getSeparator());
        ProtoVFile prev = root();
        boolean makingNew = false;
        while (tkz.hasMoreTokens()) {
            final String pathPart = tkz.nextToken();
            if (makingNew) {
                prev = mkDir(prev, pathPart);
            } else {
                final ProtoVFile child = prev.child(pathPart);
                if (child != null) {
                    if (!child.isDir()) {
                        return null;
                    } else {
                        prev = child;
                    }
                } else {
                    prev = mkDir(prev, pathPart);
                    makingNew = true;
                }
            }
        }
        return prev;
    }

    public boolean rm(final VFile vfile) {
        if (vfile.isDir() && vfile.list().iterator().hasNext()) {
            return false;
        }
        final ProtoVFile child = resolve(vfile);
        return deleteChild(getParent(child), child);
    }

    ProtoVFile resolve(final VFile vfile) {
        return resolve(vfile.getAbsoluteName());
    }

    private boolean deleteChild(final ProtoVFile parent, final ProtoVFile child) {
        if (parent == null) {
            if (child.getName().equals(rootName)) {
                return false;
            } else {
                throw new IllegalStateException("parent is null, but child is not root : " + child);
            }
        }
        final Iterable<ProtoVFile> allChildren = list(parent);
        final OutputStream parentDataStream = device.openWriter(parent.getProtoNode().getDataBlockNo()).asStream();
        try {
            for (final ProtoVFile anyChild : allChildren) {
                if (anyChild.getProtoNode().getNo() != child.getProtoNode().getNo()) {
                    final VFS.DirEntry entry = VFS.DirEntry.newBuilder()
                            .setInode(anyChild.getProtoNode().getNo())
                            .setName(anyChild.getProtoNode().getName()).build();
                    writeDirEntryTo(entry, parentDataStream);
                } else {
                    log.debug("not writing '{}' as it's deleted ", child.getName());
                }
            }
        } finally {
            try {
                parentDataStream.close();
            } catch (IOException e) {
                throw new VFSException(e);
            }
        }

        final VFS.Node childProto = child.getProtoNode();
        device.freeStartingWith(childProto.getNo());
        device.freeStartingWith(childProto.getDataBlockNo());
        return true;
    }

    String getSeparator() {
        return cfg.getSeparator();
    }

    char getSeparatorChar() {
        return cfg.getSeparatorChar();
    }
}
