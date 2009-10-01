package org.nuxeo.ecm.spaces.core.impl.exceptions;

public class NoElementFoundException extends Exception {

  private static final long serialVersionUID = 1L;

  public NoElementFoundException() {
  }

  public NoElementFoundException(String message) {
    super(message);
  }

  public NoElementFoundException(Throwable cause) {
    super(cause);
  }

  public NoElementFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
