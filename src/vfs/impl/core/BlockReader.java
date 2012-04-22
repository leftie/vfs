package vfs.impl.core;

import java.io.IOException;
import java.io.InputStream;

public class BlockReader {
    private final InputStream stream;

    BlockReader(final InputStream stream) {
        this.stream = stream;
    }

    public InputStream asStream() {
        return stream;
    }

    public void close() {
        try {
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
