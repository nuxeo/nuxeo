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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DocumentTranslationMap.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Translation map for exported/imported documents.
 */
public interface DocumentTranslationMap {

    /**
     * Returns the old documents server name.
     */
    String getOldServerName();

    /**
     * Returns the new documents server name.
     */
    String getNewServerName();

    /**
     * Returns the unmodifiable map of document references.
     */
    Map<DocumentRef, DocumentRef> getDocRefMap();

    void put(DocumentRef oldRef, DocumentRef newRef);

    void putAll(Map<DocumentRef, DocumentRef> refs);

}
