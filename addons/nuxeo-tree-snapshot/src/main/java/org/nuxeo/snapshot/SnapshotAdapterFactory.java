package org.nuxeo.snapshot;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

public class SnapshotAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> adapter) {

        if (Snapshotable.class.getName().equals(adapter.getName())) {
            if (doc.hasFacet(Snapshotable.FACET)) {
                return new SnapshotableAdapter(doc);
            }
        }

        if (Snapshot.class.getName().equals(adapter.getName())) {
            if (doc.hasFacet(Snapshot.FACET)) {
                return new SnapshotableAdapter(doc);
            }
        }

        return null;
    }

}
