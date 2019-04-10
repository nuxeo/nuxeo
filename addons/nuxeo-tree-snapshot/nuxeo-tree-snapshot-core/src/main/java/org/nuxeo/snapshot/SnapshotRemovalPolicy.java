/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
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

    protected boolean canRemoveVersions(CoreSession session, DocumentModel doc, List<String> uuids) {
        IterableQueryResult result = null;
        try {
            StringBuffer nxql = new StringBuffer("select ecm:uuid from Document where ");
            nxql.append(SnapshotableAdapter.CHILDREN_PROP + "/* IN (");
            for (int i = 0; i < uuids.size(); i++) {
                if (i > 0) {
                    nxql.append(",");
                }
                nxql.append("'" + uuids.get(i) + "'");
            }
            nxql.append(")");
            result = session.queryAndFetch(nxql.toString(), NXQL.NXQL, QueryFilter.EMPTY);
            if (result.iterator().hasNext()) {
                return false;
            }
            return true;
        } finally {
            if (result != null) {
                result.close();
            }
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getRemovableVersionIds(CoreSession session, ShallowDocumentModel deletedLiveDoc,
            List<String> versionUUIDs) {

        if (!canRemoveVersions(session, deletedLiveDoc, versionUUIDs)) {
            return Collections.emptyList();
        }
        return versionUUIDs;
    }
}
