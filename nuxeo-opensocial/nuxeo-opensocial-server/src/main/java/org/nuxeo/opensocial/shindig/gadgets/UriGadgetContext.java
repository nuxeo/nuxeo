package org.nuxeo.opensocial.shindig.gadgets;

import java.net.URI;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetContext;

public class UriGadgetContext extends GadgetContext {
  private URI uri;
  public UriGadgetContext(URI uri) {
    this.uri =uri;
  }

  public URI getUrl() {
    return uri;
  }

  @Override
  public SecurityToken getToken() {
    return null;
  }
}
