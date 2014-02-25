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
package org.nuxeo.ecm.collections.core;

import java.util.List;

import org.nuxeo.ecm.collections.adapter.Collection;
import org.nuxeo.ecm.collections.adapter.CollectionMember;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 *
 * @since 5.9.3
 */
public class CollectionManagerImpl extends DefaultComponent implements
        CollectionManager {

    @Override
    public void addToCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeAdded) {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

    @Override
    public void addToCollection(final DocumentModel collection,
            final DocumentModel documentToBeAdded) throws ClientException {
        Collection colAdapter = collection.getAdapter(Collection.class);
        colAdapter.addDocument(documentToBeAdded.getId());
        collection.getCoreSession().saveDocument(colAdapter.getDocument());

        new UnrestrictedSessionRunner(documentToBeAdded.getCoreSession()) {

            @Override
            public void run() throws ClientException {
                CollectionMember docAdapter = documentToBeAdded.getAdapter(CollectionMember.class);
                docAdapter.addToCollection(collection.getId());
                documentToBeAdded.getCoreSession().saveDocument(
                        docAdapter.getDocument());
            }

        }.runUnrestricted();
    }

    @Override
    public void removeFromCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeRemoved) {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFromCollection(final DocumentModel collection,
            final DocumentModel documentToBeRemoved) {
        // TODO Auto-generated method stub
        //
        throw new UnsupportedOperationException();
    }

    public boolean isCollectable(DocumentModel doc) {
        return doc.hasFacet(CollectionConstants.COLLECTABLE_FACET);
    }

}
