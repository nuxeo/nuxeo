package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import static org.nuxeo.ecm.spaces.core.impl.Constants.*;

public class BeansDocumentAdapterFactory implements DocumentAdapterFactory {

  @SuppressWarnings("unchecked")
  public Object getAdapter(DocumentModel doc, Class itf) {
    if (doc.getType()
        .equals(Univers.TYPE)) {
      return new UniversDocumentWrapper(doc);
    } else if (doc.getType()
        .equals(Space.TYPE)) {
      return new SpaceDocumentWrapper(doc);
    } else if (doc.getType()
        .equals(Gadget.TYPE)) {
      return new GadgetDocumentWrapper(doc);
    }
    return null;
  }

}
