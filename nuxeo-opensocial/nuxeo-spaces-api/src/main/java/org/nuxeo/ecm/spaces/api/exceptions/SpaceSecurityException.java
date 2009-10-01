package org.nuxeo.ecm.spaces.api.exceptions;

public class SpaceSecurityException extends SpaceException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public SpaceSecurityException() {
  }

  public SpaceSecurityException(String message, Throwable cause) {
    super(message, cause);
  }

  public SpaceSecurityException(String message) {
    super(message);
  }

  public SpaceSecurityException(Throwable cause) {
    super(cause);
  }

}
