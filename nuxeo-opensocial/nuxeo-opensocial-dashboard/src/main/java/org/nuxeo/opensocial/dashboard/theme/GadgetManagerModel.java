package org.nuxeo.opensocial.dashboard.theme;

import java.util.List;

import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.theme.models.AbstractModel;

public class GadgetManagerModel extends AbstractModel {

  private List<String> categories;
  private List<GadgetDeclaration> gadgets;
  private boolean anonymous;

  public boolean isAnonymous() {
    return anonymous;
  }

  public void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }

  public GadgetManagerModel(List<String> categories,
      List<GadgetDeclaration> gadgets) {
    this.categories = categories;
    this.gadgets = gadgets;
  }

  public List<String> getCategories() {
    return categories;
  }

  public List<GadgetDeclaration> getGadgets() {
    return gadgets;
  }

}
