package org.nuxeo.ecm.platform.rendition.version;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;
import org.nuxeo.ecm.platform.rendition.Constants;

public class RenditionAwareVersionRemovalPolicy implements
        OrphanVersionRemovalFilter {

    @Override
    public List<String> getRemovableVersionIds(CoreSession session,
            ShallowDocumentModel deletedLiveDoc, List<String> versionUUIDs) {

        if (deletedLiveDoc.hasFacet(Constants.RENDITION_FACET)) {
            // don't remove orphan rendition versions
            return Collections.emptyList();
        }
        return versionUUIDs;
    }
}
