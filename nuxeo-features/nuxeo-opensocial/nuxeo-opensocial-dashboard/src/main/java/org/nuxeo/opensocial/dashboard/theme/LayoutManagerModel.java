package org.nuxeo.opensocial.dashboard.theme;

import org.nuxeo.theme.models.AbstractModel;

public class LayoutManagerModel extends AbstractModel {

  private boolean anonymous;

  public boolean isAnonymous() {
    return anonymous;
  }

  public void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }

  public LayoutManagerModel(){

  }

}
