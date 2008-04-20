/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository.impl;

import java.util.Hashtable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.DocumentProvider;
import org.nuxeo.ecm.core.api.repository.cache.DocumentModelCache;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Max Stepanov
 *
 */
public class DocumentProviderManager extends DefaultComponent implements DocumentProvider {

    private final Map<String, DocumentModel> cache = new Hashtable<String, DocumentModel>();
    private final Map<DocumentRef, String> refs = new Hashtable<DocumentRef, String>();

    private CoreSession session;

    public void setSession(CoreSession session) {
        this.session = session;
    }

    public CoreSession getSession() {
        return session;
    }

    public void removeDocumentFromCache(String id) {
        cache.remove(id);
    }

    public void removeDocumentFromCache(DocumentRef ref) {
        ((DocumentModelCache)session).uncacheDocument(ref);
    }

    public DocumentModel getCachedDocument(String id) {
        return ((DocumentModelCache)session).getCachedDocument(new IdRef(id));
    }

    public DocumentModel getCachedDocument(DocumentRef ref) {
        return ((DocumentModelCache)session).getCachedDocument(ref);
    }

    private DocumentModel cacheDocument(DocumentModel docModel) {
        return ((DocumentModelCache)session).cacheDocument(docModel);
    }

    private DocumentModel cacheDocument(DocumentRef docRef, DocumentModel docModel) {
        System.err.println("#################### SHOULD NOT PASS HERE #################");
        return ((DocumentModelCache)session).cacheDocument(docModel);
    }

    private DocumentModelList cacheDocumentList(DocumentModelList docList) {
        int length = docList.size();
        for ( int i = 0; i < length; ++i) {
            DocumentModel docModel = docList.get(i);
            DocumentModel cachedDocModel = cacheDocument(docModel);
            if (cachedDocModel != docModel) {
                docList.set(i, cachedDocModel);
            }
        }
        return docList;
    }

    // FIXME: this method recurses infinitely.
    public DocumentModel getDocument(String id) throws ClientException {
        return getDocument(id);
    }

    public DocumentModel getDocument(DocumentRef docRef) throws ClientException {
        return session.getDocument(docRef);
    }

    public DocumentModel getDocument(String id, boolean force) throws ClientException {
        return getDocument(new IdRef(id), force);
    }

    public DocumentModel getDocument(DocumentRef docRef, boolean force) throws ClientException {
        if (force) {
            return ((DocumentModelCache)session).fetchDocument(docRef);
        } else {
            return session.getDocument(docRef);
        }
    }

    public DocumentModel getDocument(DocumentRef docRef, String[] schemas) throws ClientException {
        return session.getDocument(docRef);
    }

    public DocumentModel getRootDocument() throws ClientException {
        return session.getRootDocument();
    }

    public boolean exists(DocumentRef docRef) throws ClientException {
        return session.exists(docRef);
    }

    public DocumentModelList getChildren(DocumentRef parent, String type) throws ClientException {
        return session.getChildren(parent, type);
    }

    public DocumentModelList getChildren(DocumentRef parent) throws ClientException {
        return session.getChildren(parent);
    }

    public DocumentModel getChild(DocumentRef parent, String name) throws ClientException {
        return session.getChild(parent, name);
    }

    /**
     * @return the parent document or null if this is the root document
     * @see CoreSession#getParentDocument(DocumentRef docRef)
     */
    public DocumentModel getParentDocument(DocumentRef docRef) throws ClientException {
        return session.getParentDocument(docRef);
    }

    public DocumentModelIterator getChildrenIterator(DocumentRef parent) throws ClientException {
        return session.getChildrenIterator(parent);
    }

}
