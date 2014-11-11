package org.nuxeo.opensocial.dashboard.theme;

import org.nuxeo.theme.models.AbstractModel;

public class MainContentModel extends AbstractModel {

  private boolean isAnonymous;

  public boolean getIsAnonymous() {
    return isAnonymous;
  }

  public void setAnonymous(boolean isAnonymous) {
    this.isAnonymous = isAnonymous;
  }

}
