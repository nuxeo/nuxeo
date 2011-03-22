/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository.cache;

import java.util.Iterator;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CachingDocumentIterator implements DocumentModelIterator {

    private static final long serialVersionUID = -2658570878403151252L;

    private final DocumentModelCache cache;
    private final DocumentModelIterator it;

    public CachingDocumentIterator(DocumentModelCache cache, DocumentModelIterator it) {
        this.it = it;
        this.cache = cache;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public DocumentModel next() {
        return cache.cacheDocument(it.next());
    }

    @Override
    public void remove() {
        //TODO remove from cache too
        it.remove();
    }

    @Override
    public Iterator<DocumentModel> iterator() {
        return this;
    }

    @Override
    public long size() {
        return it.size();
    }

}
