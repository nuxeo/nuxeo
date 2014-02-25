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
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.3
 */
public interface CollectionManager {

    void addToCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeAdded) throws ClientException;

    void addToCollection(final DocumentModel collection,
            final DocumentModel documentToBeAdded) throws ClientException;

    void removeFromCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeRemoved) throws ClientException;

    void removeFromCollection(final DocumentModel collection,
            final DocumentModel documentToBeRemoved) throws ClientException;

    boolean isCollectable(final DocumentModel doc);

}
