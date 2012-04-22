package util.io;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class DigestingInputStream extends InputStream {  //todo: review

    private final InputStream delegate;
    private MessageDigest digest;

    public DigestingInputStream(final InputStream delegate, final MessageDigest digest) {
        this.delegate = delegate;
        this.digest = digest;
    }

    private final byte[] buf = new byte[1];

    public int read() throws IOException {
        int res = delegate.read();
        if(res != -1) {
            buf[0] = (byte) (res & 0xFF);
            digest.update(buf);
        }
        return res;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        int res = delegate.read(b, off, len);
        if (res != -1) {
            digest.update(b, off, res);
        }
        return res;
    }

    public long skip(long n) throws IOException {
        return 0;
    }

    public int available() throws IOException {
        return delegate.available();
    }

    public void close() throws IOException {
        delegate.close();
    }

    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException(this + " does not support marks");
    }

    public synchronized void reset() throws IOException {
        delegate.reset();
    }

    public boolean markSupported() {
        return false;
    }

    public MessageDigest getDigest() {
        return digest;
    }

    public String getDigestAsHex() {
        final BigInteger number = new BigInteger(1, digest.digest());
        return String.format("%032x", number);
    }
}
