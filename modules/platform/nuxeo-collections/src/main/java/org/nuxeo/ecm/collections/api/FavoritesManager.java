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
package org.nuxeo.ecm.collections.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.4
 */
public interface FavoritesManager {

    void addToFavorites(final DocumentModel document, final CoreSession session);

    boolean canAddToFavorites(final DocumentModel document);

    /**
     * @deprecated since 10.3 use {@link #getFavorites(CoreSession)} instead
     */
    @Deprecated
    DocumentModel getFavorites(final DocumentModel context, final CoreSession session);

    DocumentModel getFavorites(final CoreSession session);

    boolean isFavorite(final DocumentModel document, final CoreSession session);

    void removeFromFavorites(final DocumentModel document, final CoreSession session);

}
