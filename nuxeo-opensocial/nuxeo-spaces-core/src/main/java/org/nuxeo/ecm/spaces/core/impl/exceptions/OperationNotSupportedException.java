package org.nuxeo.ecm.spaces.core.impl.exceptions;

public class OperationNotSupportedException extends Exception {

  private static final long serialVersionUID = 1L;

  public OperationNotSupportedException() {
  }

  public OperationNotSupportedException(String message) {
    super(message);
  }

  public OperationNotSupportedException(Throwable cause) {
    super(cause);
  }

  public OperationNotSupportedException(String message, Throwable cause) {
    super(message, cause);
  }

}
