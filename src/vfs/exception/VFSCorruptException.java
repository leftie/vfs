package vfs.exception;

public class VFSCorruptException extends VFSException {

    public VFSCorruptException() {
        super();
    }

    public VFSCorruptException(final String message) {
        super(message);
    }

    public VFSCorruptException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VFSCorruptException(final Throwable cause) {
        super(cause);
    }
}
