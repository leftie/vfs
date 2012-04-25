package vfs.impl.core;

import java.io.IOException;
import java.io.InputStream;

public class DataInput {
    private final InputStream stream;

    DataInput(final InputStream stream) {
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
