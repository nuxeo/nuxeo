/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.4
 */
public interface FavoritesManager {

    void addToFavorites(final DocumentModel document, final CoreSession session)
            throws ClientException;

    boolean canAddToFavorites(final DocumentModel document)
            throws ClientException;

    DocumentModel getFavorites(final DocumentModel context,
            final CoreSession session) throws ClientException;

    boolean isFavorite(final DocumentModel document, final CoreSession session)
            throws ClientException;

    void removeFromFavorites(final DocumentModel document,
            final CoreSession session) throws ClientException;

}
