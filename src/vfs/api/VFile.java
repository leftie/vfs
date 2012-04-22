package vfs.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vfs.exception.VFSException;

import java.io.InputStream;
import java.io.OutputStream;

public interface VFile {

    boolean isFile();

    boolean isDir();

    String getName();

    String getAbsoluteName();

    @Nullable VFile getParent() throws VFSException;

    Iterable<VFile> list() throws VFSException;

    @Nullable VFile child(@NotNull final String name) throws VFSException;

    InputStream openInput() throws VFSException;

    OutputStream openOutput() throws VFSException;

}
