package org.nuxeo.snapshot;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.versioning.DefaultVersionRemovalPolicy;
import org.nuxeo.ecm.core.versioning.VersionRemovalPolicy;

public class SnapshotRemovalPolicy extends DefaultVersionRemovalPolicy
        implements VersionRemovalPolicy {

    protected boolean canRemoveVersions(Session session, Document doc) {
        IterableQueryResult result = null;
        try {
            List<String> uuids = doc.getVersionsIds();
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

    @Override
    public void removeVersions(Session session, Document doc,
            CoreSession coreSession) throws ClientException {
        // don't remove orphans for uuids part of a snapshot
        if (canRemoveVersions(session, doc)) {
            super.removeVersions(session, doc, coreSession);
        }
    }
}
