package vfs.exception;

public class VFSException extends RuntimeException {

    public VFSException() {
        super();
    }

    public VFSException(final String message) {
        super(message);
    }

    public VFSException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VFSException(final Throwable cause) {
        super(cause);
    }
}
