package util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class DigestingOutputStream extends OutputStream { //todo: review
    private final OutputStream delegate;
    private MessageDigest digest;

    public DigestingOutputStream(final OutputStream delegate, final MessageDigest digest) {
        this.delegate = delegate;
        this.digest = digest;
    }

    private final byte[] buf = new byte[1];

    public void write(final int b) throws IOException {
        buf[0] = (byte) b;
        digest.update(buf, 0, 1);
        delegate.write(b);
    }

    public void write(final byte b[]) throws IOException {
        digest.update(b);
        delegate.write(b);
    }

    public void write(final byte b[], final int off, final int len) throws IOException {
        digest.update(b, off, len);
        delegate.write(b, off, len);
    }

    public void flush() throws IOException {
        delegate.flush();
    }

    public void close() throws IOException {
        delegate.close();
    }

    public MessageDigest getDigest() {
        return digest;
    }

    public String getDigestAsHex() {
        final BigInteger number = new BigInteger(1, digest.digest());
        return String.format("%032x", number);
    }
}
