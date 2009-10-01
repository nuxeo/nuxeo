package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.Univers;

public class UniversDocumentWrapper extends DocumentWrapper implements Univers {

  UniversDocumentWrapper(DocumentModel doc) {
    super(doc);
  }

  public boolean isEqualTo(Univers univers) {
    return univers.getId()!=null && univers.getId().equals(getId());
  }


}
