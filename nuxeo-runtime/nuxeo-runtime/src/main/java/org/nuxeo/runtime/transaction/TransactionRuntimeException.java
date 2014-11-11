package org.nuxeo.runtime.transaction;

/**
 * Re-ified checked errors caught while operating the transaction. The error is
 * logged but the error condition is re-throwed as a runtime, enabling
 * transaction helper callers being able to be aware about the error..
 * 
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 *
 */
public class TransactionRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransactionRuntimeException(String msg, Throwable error) {
        super(msg, error);
    }

}
