package vfs.impl.proto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.collections.Cu;
import vfs.api.VFile;
import vfs.exception.VFSException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;


final class ProtoVFile implements VFile {

    private final ProtoVFS fs;
    private final String name;
    private final NodeFlags flags;
    private final VFS.Node protoNode;

    private final String absolutePath;

    ProtoVFile(final ProtoVFS fs, final String name, final String absolutePath, final NodeFlags flags, final VFS.Node protoNode) {
        this.fs = fs;
        this.name = name;
        this.flags = flags;
        this.protoNode = protoNode;
        this.absolutePath = absolutePath;
    }

    @Override
    public boolean isFile() {
        return flags.isFile();
    }

    @Override
    public boolean isDir() {
        return flags.isDir();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @Nullable
    public ProtoVFile child(final String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException();
        }
        for (final ProtoVFile candidate : fs.list(this)) {
            if (candidate.getName().equals(name)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public VFile getParent() {
        return fs.getParent(this);
    }

    @Override
    public String getAbsolutePath() {
        return absolutePath;
    }

    @NotNull
    @Override
    public Iterable<VFile> list() {
        final Iterator<ProtoVFile> children = fs.list(fs.resolve(this.getAbsolutePath())).iterator();
        return Cu.iterable(
                new Iterator<VFile>() {
                    @Override
                    public boolean hasNext() {
                        return children.hasNext();
                    }

                    @Override
                    public ProtoVFile next() {
                        return children.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                }
        );
    }

    VFS.Node getProtoNode() {
        return protoNode;
    }

    @Override
    public InputStream openFileInput() throws VFSException {
        return fs.openInput(fs.resolve(this.getAbsolutePath()));
    }

    @Override
    public OutputStream openFileOutput() throws VFSException {
        return fs.openOutput(fs.resolve(this.getAbsolutePath()));
    }

    @Override
    public String toString() {
        return "ProtoVFile{" + absolutePath + "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ProtoVFile that = (ProtoVFile) o;

        if (!absolutePath.equals(that.absolutePath)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return absolutePath.hashCode();
    }
}
