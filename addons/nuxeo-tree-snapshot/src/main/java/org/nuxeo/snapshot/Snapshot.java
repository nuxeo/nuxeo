package org.nuxeo.snapshot;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public interface Snapshot extends Snapshotable {

    public static final String FACET = "Snapshot";

    List<DocumentModel> getChildren();

    List<Snapshot> getChildrenSnapshots();

    List<Snapshot> getFlatTree();

    DocumentModel restore(String versionLabel);

    DocumentModel getDocument();

    DocumentRef getRef();

}
