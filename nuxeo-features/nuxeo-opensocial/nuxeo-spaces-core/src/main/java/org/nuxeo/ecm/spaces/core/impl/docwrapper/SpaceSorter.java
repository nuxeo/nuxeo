package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.spaces.api.Space;

public class SpaceSorter implements Sorter {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public int compare(DocumentModel o1, DocumentModel o2) {

    Calendar d1 = o1.getAdapter(Space.class)
        .getDatePublication();
    Calendar d2 = o2.getAdapter(Space.class)
        .getDatePublication();
    if (d1 == null && d2 == null)
      return 0;
    else if (d1 == null)
      return -1;
    else if (d2 == null)
      return 1;
    return d1.compareTo(d2);
  }
}
