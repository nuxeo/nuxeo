package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.core.impl.Constants;

public class SpaceDocumentWrapper extends DocumentWrapper implements Space {

  SpaceDocumentWrapper(DocumentModel doc) {
    super(doc);
  }

  public String getLayout() {
    return getInternalStringProperty(Constants.Space.SPACE_LAYOUT);
  }

  public String getCategory() {
    return getInternalStringProperty(Constants.Space.SPACE_CATEGORY);
  }

  public boolean isEqualTo(Space space) {
    return space.getId()!=null && space.getId().equals(getId());
  }

  public String getTheme() {
    return getInternalStringProperty(Constants.Space.SPACE_THEME);
  }

}
