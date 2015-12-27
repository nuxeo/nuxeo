/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
