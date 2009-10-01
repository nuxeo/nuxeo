package org.nuxeo.opensocial.container.factory.utils;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetContext;

public class NxGadgetContext extends GadgetContext {

  private String gadgetDef;
  private SecurityToken securityToken;

  public NxGadgetContext(String gadgetDef, String viewer, String owner) {
    this.gadgetDef = gadgetDef;
    this.securityToken = new NxSecurityToken(viewer, owner);
  }

  @Override
  public URI getUrl() {
    try {
      return new URI(gadgetDef);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public SecurityToken getToken() {
    return this.securityToken;
  }

}
