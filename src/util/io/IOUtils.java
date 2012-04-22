package util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtils { //todo:review

    public static byte[] readInputStreamToBytes(final InputStream in) throws IOException {
        return readInputStreamToBytes(in, 0);
    }

    public static byte[] readInputStreamToBytes(final InputStream in, final int noMoreThanThisBytes) throws IOException {
        final boolean unlimited = 0 == noMoreThanThisBytes;

        final ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(65536, in.available()));

        byte[] buf = new byte[65536];
        int n;
        int remaining = noMoreThanThisBytes;
        while (0 < (n = in.read(
                buf, 0,
                unlimited ? buf.length : Math.min(remaining, buf.length)))) {
            bos.write(buf, 0, n);
            remaining -= n;
        }

        bos.close();
        return bos.toByteArray();
    }
}
