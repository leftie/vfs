package vfs.impl.proto;

import org.jetbrains.annotations.NotNull;
import vfs.api.VFile;
import vfs.api.VFileManager;

import java.util.StringTokenizer;

class ProtoFileManager implements VFileManager {

    private final ProtoVFS vfs;

    ProtoFileManager(final ProtoVFS vfs) {
        this.vfs = vfs;
    }

    @Override
    public ProtoVFile mkDir(@NotNull final VFile parentDir, @NotNull final String dirName) {
        return vfs.mkDir(vfs.resolve(parentDir), checkPath(dirName));
    }

    @Override
    public ProtoVFile touch(@NotNull final VFile parentDir, @NotNull final String fileName) {
        return vfs.touch(vfs.resolve(parentDir), checkPath(fileName));
    }

    @Override
    public VFile mkDirs(@NotNull final String fullPath) {
        return vfs.mkDirs(checkFullPath(fullPath));
    }

    @Override
    public VFile resolve(@NotNull final String fullPath) {
        return vfs.resolve(checkFullPath(fullPath));
    }

    @Override
    public boolean rm(@NotNull final VFile vfile) {
        return vfs.rm(vfile);
    }


    private String checkFullPath(String fullPath) throws IllegalArgumentException {
        fullPath = vfs.normalize(fullPath);
        if (!fullPath.startsWith(vfs.getSeparator())) {
            throw new IllegalArgumentException("is not a full path :" + fullPath);
        }
        final StringTokenizer tkz = new StringTokenizer(fullPath, vfs.getSeparator());
        while (tkz.hasMoreTokens()) {
            if (!isNameOk(tkz.nextToken())) {
                throw new IllegalArgumentException("name is not ok :" + fullPath);
            }
        }
        return fullPath;
    }

    private String checkPath(final String path) {
        if (isNameOk(path)) {
            return path;
        } else {
            throw new IllegalArgumentException("path is not ok :" + path);
        }
    }

    private boolean isNameOk(String name) {
        name = name.trim();
        if (name.isEmpty()) {
            return false;
        }
        if ("..".equals(name) || ".".equals(name)) {
            return false;
        }
        if (name.indexOf(vfs.getSeparatorChar()) >= 0) {
            return false;
        }
        return true;
    }
}
