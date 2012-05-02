package org.nuxeo.snapshot;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.VersioningOption;

public interface Snapshotable {

    public static final String FACET = "Snapshotable";

    Snapshot createSnapshot(VersioningOption option) throws ClientException;

}
