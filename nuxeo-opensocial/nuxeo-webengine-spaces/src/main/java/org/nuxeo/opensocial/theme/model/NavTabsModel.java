package org.nuxeo.opensocial.theme.model;

import org.nuxeo.theme.models.AbstractModel;

public class NavTabsModel extends AbstractModel {


  private boolean anonymous;

  public boolean isAnonymous() {
    return anonymous;
  }

  public void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }


}
