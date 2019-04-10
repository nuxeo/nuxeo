package org.nuxeo.snapshot;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public interface Snapshot extends Snapshotable {

    public static final String FACET = "Snapshot";

    List<DocumentModel> getChildren() throws ClientException;

    List<Snapshot> getChildrenSnapshots() throws ClientException;

    List<Snapshot> getFlatTree() throws ClientException;

    DocumentModel restore(String versionLabel) throws ClientException;

    DocumentModel getDocument();

    DocumentRef getRef();

}
