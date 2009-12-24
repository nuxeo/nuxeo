package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.spaces.api.Space;

public class SpaceSorter implements Sorter {

    private static final long serialVersionUID = 1L;

    public int compare(DocumentModel o1, DocumentModel o2) {
        Calendar d1, d2;
        try {
            Space s1 = o1.getAdapter(Space.class);
            Space s2 = o2.getAdapter(Space.class);
            if (s1 == null || s2 == null) {
                return 0;
            }

            d1 = s1.getDatePublication();
            d2 = s2.getDatePublication();

            if (d1 == null && d2 == null)
                return 0;

            else if (d1 == null)
                return -1;
            else if (d2 == null)
                return 1;

        } catch (ClientException e) {
            return 0;
        }
        return d1.compareTo(d2);
    }
}
