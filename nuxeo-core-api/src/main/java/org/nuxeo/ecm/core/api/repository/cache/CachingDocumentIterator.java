/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public boolean hasNext() {
        return it.hasNext();
    }

    public DocumentModel next() {
        return cache.cacheDocument(it.next());
    }

    public void remove() {
        //TODO remove from cache too
        it.remove();
    }

    public Iterator<DocumentModel> iterator() {
        return CachingDocumentIterator.this;
    }

    public long size() {
        return it.size();
    }

}
