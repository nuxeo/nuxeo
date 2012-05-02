package org.nuxeo.snapshot;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.versioning.DefaultVersionRemovalPolicy;
import org.nuxeo.ecm.core.versioning.VersionRemovalPolicy;

public class SnapshotRemovalPolicy extends DefaultVersionRemovalPolicy
        implements VersionRemovalPolicy {

    @Override
    public void removeVersions(Session session, Document doc,
            CoreSession coreSession) throws ClientException {
        // don't remove orphans for now
        return;
    }
}
