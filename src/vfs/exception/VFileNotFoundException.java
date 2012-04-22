package vfs.exception;

public class VFileNotFoundException extends VFSException {

    public VFileNotFoundException() {
        super();
    }

    public VFileNotFoundException(final String message) {
        super(message);
    }

    public VFileNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VFileNotFoundException(final Throwable cause) {
        super(cause);
    }
}
