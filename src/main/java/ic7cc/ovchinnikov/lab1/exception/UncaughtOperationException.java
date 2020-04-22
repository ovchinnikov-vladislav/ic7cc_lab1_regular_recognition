package ic7cc.ovchinnikov.lab1.exception;

public class UncaughtOperationException extends RuntimeException {
    public UncaughtOperationException() {
        super();
    }

    public UncaughtOperationException(String message) {
        super(message);
    }

    public UncaughtOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UncaughtOperationException(Throwable cause) {
        super(cause);
    }

    protected UncaughtOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
