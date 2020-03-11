/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * Iterator over the children of a folder.
 */
public class DocumentModelChildrenIterator implements DocumentModelIterator {

    private static final Log log = LogFactory.getLog(DocumentModelChildrenIterator.class);

    private CoreSession session;

    private String type;

    private Filter filter;

    private Iterator<String> it;

    private DocumentModel next;

    public DocumentModelChildrenIterator(CoreSession session, DocumentRef parentRef, String type, Filter filter)
            {
        this.session = session;
        this.type = type;
        this.filter = filter;

        // fetch all the children ids now
        List<DocumentRef> refs = session.getChildrenRefs(parentRef, null);
        List<String> ids = new ArrayList<>(refs.size());
        for (DocumentRef ref : refs) {
            ids.add(ref.toString()); // always an IdRef
        }
        it = ids.iterator();
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        // this could be optimized to retrieve batches of documents instead of
        // one at a time; this would need to call SQLSession.getDocumentsById
        // through a new CoreSession API (or improve getDocuments)
        for (;;) {
            if (!it.hasNext()) {
                return false;
            }
            String id = it.next();
            IdRef idRef = new IdRef(id);
            if (!session.exists(idRef)) {
                continue;
            }
            DocumentModel doc;
            doc = session.getDocument(idRef);
            if (accept(doc)) {
                next = doc;
                return true;
            }
            // continue
        }
    }

    private boolean accept(DocumentModel doc) {
        if (type != null && !type.equals(doc.getType())) {
            return false;
        }
        if (filter != null && !filter.accept(doc)) {
            return false;
        }
        return true;
    }

    @Override
    public DocumentModel next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        DocumentModel res = next;
        next = null;
        return res;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<DocumentModel> iterator() {
        return this; // NOSONAR this iterable does not support multiple traversals
    }

    @Override
    public long size() {
        return UNKNOWN_SIZE;
    }

}
