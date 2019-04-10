package org.nuxeo.snapshot;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.VersioningOption;

public interface Snapshotable {

    public static final String FACET = "Snapshotable";

    public static final String ABOUT_TO_CREATE_LEAF_VERSION_EVENT = "aboutToCreateLeafVersionEvent";

    public static final String ROOT_DOCUMENT_PROPERTY = "leafRootDocument";

    Snapshot createSnapshot(VersioningOption option) throws ClientException;

}
