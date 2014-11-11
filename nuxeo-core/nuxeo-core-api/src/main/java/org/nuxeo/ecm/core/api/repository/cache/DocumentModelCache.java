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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 *
 */
public interface DocumentModelCache {

    DocumentModel cacheDocument(DocumentModel doc);

    DocumentModel uncacheDocument(DocumentRef ref);

    DocumentModel getCachedDocument(DocumentRef ref);

    void flushDocumentCache();

    DocumentModel fetchDocument(DocumentRef ref) throws ClientException;

    void cacheChildren(DocumentRef parent, DocumentModelList children);

    void uncacheChildren(DocumentRef parent);

    DocumentModelList  fetchChildren(DocumentRef parent) throws Exception;

    DocumentModelList  getCachedChildren(DocumentRef parent) throws ClientException;

    void cacheChild(DocumentRef parent, DocumentRef child);

    void uncacheChild(DocumentRef parent, DocumentRef child);

    DocumentModelList fetchAndCacheChildren(DocumentRef parent) throws ClientException;

    void handleDirtyUpdateTag(Object tag);

    void addListener(DocumentModelCacheListener listener);

    void removeListener(DocumentModelCacheListener listener);

}
