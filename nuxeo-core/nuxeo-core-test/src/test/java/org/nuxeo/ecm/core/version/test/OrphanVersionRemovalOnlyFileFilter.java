package org.nuxeo.ecm.core.version.test;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;

public class OrphanVersionRemovalOnlyFileFilter implements
        OrphanVersionRemovalFilter {

    @Override
    public List<String> getRemovableVersionIds(CoreSession session,
            ShallowDocumentModel deletedLiveDoc, List<String> versionUUIDs) {

        if (deletedLiveDoc.getType().equals("File")) {
            return Collections.emptyList();
        }

        return versionUUIDs;
    }

}
