/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.trash;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Trash service delegating to two different backends, for use during migration.
 *
 * @since 10.2
 */
public class BridgeTrashService extends AbstractTrashService {

    protected final TrashService first;

    protected final TrashService second;

    public BridgeTrashService(TrashService first, TrashService second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean isTrashed(CoreSession session, DocumentRef doc) {
        return first.isTrashed(session, doc) || second.isTrashed(session, doc);
    }

    @Override
    public void trashDocuments(List<DocumentModel> docs) {
        // write to second only
        second.trashDocuments(docs);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<DocumentRef> undeleteDocuments(List<DocumentModel> docs) {
        Set<DocumentRef> refs = new HashSet<>();
        refs.addAll(second.undeleteDocuments(docs));
        // write to first (basically lifeCycle service) to put documents in project lifeCycle state
        refs.addAll(first.undeleteDocuments(docs));
        return refs;
    }

    @Override
    public boolean hasFeature(Feature feature) {
        switch (feature) {
            case TRASHED_STATE_IS_DEDICATED_PROPERTY:
                return false;
            default:
                throw new UnsupportedOperationException(feature.name());
        }
    }

}
