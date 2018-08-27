/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.collections.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 10.3
 */
public interface CollectionLocationService {

    /**
     * Provide the default document where will be stored collections. The document must be a "Folderish" accepting
     * "Collection" sub-type.
     *
     * @param session the core session
     * @return the default location for collections
     */
    DocumentModel getUserDefaultCollectionsRoot(CoreSession session);

    /**
     * Provide the default favorites document. The document must be of type Favorites.
     *
     * @param session the core session
     * @return the favorites
     */
    DocumentModel getUserFavorites(CoreSession session);
}
