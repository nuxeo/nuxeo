package org.nuxeo.snapshot;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.versioning.OrphanVersionRemovalFilter;

public class SnapshotRemovalPolicy implements OrphanVersionRemovalFilter {

    protected boolean canRemoveVersions(CoreSession session, DocumentModel doc,
            List<String> uuids) {
        IterableQueryResult result = null;
        try {
            StringBuffer nxql = new StringBuffer(
                    "select ecm:uuid from Document where ");
            nxql.append(SnapshotableAdapter.CHILDREN_PROP + "/* IN (");
            for (int i = 0; i < uuids.size(); i++) {
                if (i > 0) {
                    nxql.append(",");
                }
                nxql.append("'" + uuids.get(i) + "'");
            }
            nxql.append(")");
            result = session.queryAndFetch(nxql.toString(), NXQL.NXQL,
                    QueryFilter.EMPTY);
            if (result.iterator().hasNext()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (result != null) {
                result.close();
            }
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getRemovableVersionIds(CoreSession session,
            ShallowDocumentModel deletedLiveDoc, List<String> versionUUIDs) {

        if (canRemoveVersions(session, deletedLiveDoc, versionUUIDs)) {
            return Collections.emptyList();
        }
        return versionUUIDs;
    }
}
