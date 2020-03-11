/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.listener.CollectionAsynchrnonousQuery;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.3
 */
public class RemovedCollectionMemberWork extends RemovedAbstractWork {

    public RemovedCollectionMemberWork() {
        super();
    }

    protected RemovedCollectionMemberWork(long offset) {
        super(offset);
    }

    private static final Log log = LogFactory.getLog(RemovedCollectionMemberWork.class);

    private static final long serialVersionUID = 6944563540430297431L;

    public static final String CATEGORY = "removedCollectionMember";

    protected static final String TITLE = "Remove CollectionMember Work";

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    protected String getQuery() {
        return CollectionAsynchrnonousQuery.QUERY_FOR_COLLECTION_MEMBER_REMOVED;
    }

    @Override
    protected void updateDocument(final DocumentModel collection) {
        log.trace(String.format("Worker %s, updating Collection %s", getId(), collection.getTitle()));

        Collection collectionAdapter = collection.getAdapter(Collection.class);
        collectionAdapter.removeDocument(docId);
        session.saveDocument(collection);
    }

}
