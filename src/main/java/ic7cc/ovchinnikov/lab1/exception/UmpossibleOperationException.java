package ic7cc.ovchinnikov.lab1.exception;

public class UmpossibleOperationException extends RuntimeException {
    public UmpossibleOperationException() {
        super();
    }

    public UmpossibleOperationException(String message) {
        super(message);
    }

    public UmpossibleOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UmpossibleOperationException(Throwable cause) {
        super(cause);
    }

    protected UmpossibleOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
