package vfs.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface VFileManager {

    @Nullable VFile mkDir(@NotNull final VFile parentDir, @NotNull final String dirName);

    @Nullable VFile touch(@NotNull final VFile parentDir, @NotNull final String fileName);

    @Nullable VFile mkDirs(@NotNull final String fullPath);

    @Nullable VFile resolve(@NotNull final String fullPath);

    boolean rm(@NotNull final VFile vfile);

}
