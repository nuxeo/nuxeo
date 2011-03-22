/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
