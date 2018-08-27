/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core;

import org.nuxeo.ecm.collections.api.CollectionLocationService;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.9.4
 */
public class FavoritesManagerImpl extends DefaultComponent implements FavoritesManager {

    @Override
    public void addToFavorites(DocumentModel document, CoreSession session) {
        final CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        collectionManager.addToCollection(getFavorites(document, session), document, session);
    }

    @Override
    public boolean canAddToFavorites(DocumentModel document) {
        final CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        return collectionManager.isCollectable(document);
    }

    @Override
    @Deprecated
    public DocumentModel getFavorites(final DocumentModel context, final CoreSession session) {
        return getFavorites(session);
    }

    @Override
    public DocumentModel getFavorites(final CoreSession session) {
        return Framework.getService(CollectionLocationService.class).getUserFavorites(session);
    }

    @Override
    public boolean isFavorite(DocumentModel document, CoreSession session) {
        final CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        return collectionManager.isInCollection(getFavorites(document, session), document, session);
    }

    @Override
    public void removeFromFavorites(DocumentModel document, CoreSession session) {
        final CollectionManager collectionManager = Framework.getService(CollectionManager.class);
        collectionManager.removeFromCollection(getFavorites(document, session), document, session);
    }

}
