package org.nuxeo.ecm.platform.rendition.version;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.versioning.DefaultVersionRemovalPolicy;
import org.nuxeo.ecm.core.versioning.VersionRemovalPolicy;
import org.nuxeo.ecm.platform.rendition.Constants;

public class RenditionAwareVersionRemovalPolicy extends
        DefaultVersionRemovalPolicy implements VersionRemovalPolicy {

    @Override
    public void removeVersions(Session session, Document doc,
            CoreSession coreSession) throws ClientException {
        if (doc.hasFacet(Constants.RENDITION_FACET)) {
            // don't remove orphan rendition versions
            return;
        }
        super.removeVersions(session, doc, coreSession);
    }
}
