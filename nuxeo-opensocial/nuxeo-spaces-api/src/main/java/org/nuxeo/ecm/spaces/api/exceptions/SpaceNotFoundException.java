package org.nuxeo.ecm.spaces.api.exceptions;


public class SpaceNotFoundException extends SpaceElementNotFoundException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public SpaceNotFoundException() {
    super();
  }

  public SpaceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public SpaceNotFoundException(String message) {
    super(message);
  }

  public SpaceNotFoundException(Throwable cause) {
    super(cause);
  }



}
