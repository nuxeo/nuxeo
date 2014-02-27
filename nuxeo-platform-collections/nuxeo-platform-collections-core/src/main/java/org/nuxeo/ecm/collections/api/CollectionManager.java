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

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.3
 */
public interface CollectionManager {

    void addToCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeAdded, final CoreSession session) throws ClientException;

    void addToCollection(final DocumentModel collection,
            final DocumentModel documentToBeAdded, final CoreSession session) throws ClientException;

    void removeFromCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeRemoved, final CoreSession session) throws ClientException;

    void removeFromCollection(final DocumentModel collection,
            final DocumentModel documentToBeRemoved, final CoreSession session) throws ClientException;

    boolean isCollectable(final DocumentModel doc);

    boolean canAddToCollection(final DocumentModel collection,
            final CoreSession session) throws ClientException;

    boolean isCollection(final DocumentModel doc);

    void addToNewCollection(String newTitle, String newDescription,
            DocumentModel documentToBeAdded, CoreSession session) throws ClientException;

}
