package org.nuxeo.opensocial.container.factory.utils;

import org.apache.shindig.auth.SecurityToken;

/**
 * Create Security Token
 * @author 10044826
 *
 */
public class NxSecurityToken implements SecurityToken {

  private String viewer;
  private String owner;

  public NxSecurityToken(String viewer, String owner) {
    this.viewer = viewer;
    this.owner = owner;
  }

  public String getAppId() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getAppUrl() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getDomain() {
    // TODO Auto-generated method stub
    return null;
  }

  public long getModuleId() {
    // TODO Auto-generated method stub
    return 0;
  }

  public String getOwnerId() {
    return owner;
  }

  public String getTrustedJson() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getUpdatedToken() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getViewerId() {
    return viewer;
  }

  public boolean isAnonymous() {
    // TODO Auto-generated method stub
    return false;
  }

}
