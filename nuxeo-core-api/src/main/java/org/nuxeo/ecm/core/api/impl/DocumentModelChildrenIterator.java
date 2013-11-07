/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.ClientException;
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

    public DocumentModelChildrenIterator(CoreSession session,
            DocumentRef parentRef, String type, Filter filter)
            throws ClientException {
        this.session = session;
        this.type = type;
        this.filter = filter;

        // fetch all the children ids now
        List<DocumentRef> refs = session.getChildrenRefs(parentRef, null);
        List<String> ids = new ArrayList<String>(refs.size());
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
            DocumentModel doc;
            try {
                doc = session.getDocument(new IdRef(id));
                if (accept(doc)) {
                    next = doc;
                    return true;
                }
            } catch (ClientException e) {
                log.error("Error retrieving next element", e);
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
        return this;
    }

    @Override
    public long size() {
        return UNKNOWN_SIZE;
    }

}
