package org.nuxeo.opensocial.container.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ContainerServiceException extends Exception implements
    IsSerializable {

  private static final long serialVersionUID = 1L;

  /**
   * Default construcor (Specification of Gwt)
   */
  public ContainerServiceException() {
  }

  public ContainerServiceException(String message, Throwable cause) {
    super(message, cause);
  }

}
