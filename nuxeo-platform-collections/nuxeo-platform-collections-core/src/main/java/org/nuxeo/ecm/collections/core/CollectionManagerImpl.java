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

import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 5.9.3
 */
public class CollectionManagerImpl extends DefaultComponent implements
        CollectionManager {

    private static final String PERMISSION_ERROR_MESSAGE = "Privilege '%s' is not granted to '%s'";

    @Override
    public void addToCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeAdded,
            final CoreSession session) throws ClientException {
        checkCanAddToCollection(collection, session);
        throw new UnsupportedOperationException();
    }

    @Override
    public void addToCollection(final DocumentModel collection,
            final DocumentModel documentToBeAdded, final CoreSession session)
            throws ClientException, DocumentSecurityException {
        checkCanAddToCollection(collection, session);
        Collection colAdapter = collection.getAdapter(Collection.class);
        colAdapter.addDocument(documentToBeAdded.getId());
        collection.getCoreSession().saveDocument(colAdapter.getDocument());

        new UnrestrictedSessionRunner(session) {

            @Override
            public void run() throws ClientException {
                CollectionMember docAdapter = documentToBeAdded.getAdapter(CollectionMember.class);
                docAdapter.addToCollection(collection.getId());
                session.saveDocument(docAdapter.getDocument());
            }

        }.runUnrestricted();
    }

    @Override
    public void removeFromCollection(final DocumentModel collection,
            final List<DocumentModel> documentListToBeRemoved,
            final CoreSession session) throws ClientException {
        checkCanAddToCollection(collection, session);
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFromCollection(final DocumentModel collection,
            final DocumentModel documentToBeRemoved, final CoreSession session)
            throws ClientException {
        checkCanAddToCollection(collection, session);
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCollectable(final DocumentModel doc) {
        return doc.hasFacet(CollectionConstants.COLLECTABLE_FACET);
    }

    @Override
    public boolean isCollection(final DocumentModel doc) {
        return doc.hasFacet(CollectionConstants.COLLECTION_FACET);
    }

    @Override
    public boolean canAddToCollection(final DocumentModel collection,
            final CoreSession session) throws ClientException {
        return isCollection(collection)
                && session.hasPermission(collection.getRef(),
                        SecurityConstants.WRITE_PROPERTIES);
    }

    public void checkCanAddToCollection(final DocumentModel collection,
            final CoreSession session) throws ClientException {
        if (!canAddToCollection(collection, session)) {
            throw new DocumentSecurityException(String.format(
                    PERMISSION_ERROR_MESSAGE,
                    CollectionConstants.CAN_COLLECT_PERMISSION,
                    session.getPrincipal().getName()));
        }
    }

    @Override
    public void addToNewCollection(final String newTitle,
            final String newDescription, final DocumentModel documentToBeAdded,
            final CoreSession session) throws ClientException {
        addToCollection(
                createCollection(newTitle, newDescription, documentToBeAdded,
                        session), documentToBeAdded, session);
    }

    protected DocumentModel createCollection(final String newTitle,
            final String newDescription, final DocumentModel documentToBeAdded,
            final CoreSession session) throws ClientException {
        DocumentModel defaultCollections = getUserDefaultCollections(
                documentToBeAdded, session);
        DocumentModel newCollection = session.createDocumentModel(
                defaultCollections.getPath().toString(), newTitle,
                CollectionConstants.COLLECTION_TYPE);
        newCollection.setProperty("dublincore", "title", newTitle);
        newCollection.setProperty("dublincore", "description", newDescription);
        return session.createDocument(newCollection);
    }

    protected DocumentModel getUserDefaultCollections(
            final DocumentModel context, final CoreSession session)
            throws ClientException {
        final UserWorkspaceService userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        final DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session, context);
        final DocumentRef lookupRef = new PathRef(
                userWorkspace.getPath().toString(),
                CollectionConstants.DEFAULT_COLLECTIONS_NAME);
        if (session.exists(lookupRef)) {
            return session.getChild(userWorkspace.getRef(),
                    CollectionConstants.DEFAULT_COLLECTIONS_NAME);
        } else {
            // does not exist yet, let's create it
            synchronized (this) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
                if (!session.exists(lookupRef)) {
                    boolean succeed = false;
                    try {
                        createDefaultCollections(session, userWorkspace);
                        succeed = true;
                    } finally {
                        if (succeed) {
                            TransactionHelper.commitOrRollbackTransaction();
                            TransactionHelper.startTransaction();
                        }
                    }
                }
                return session.getDocument(lookupRef);
            }
        }
    }

    protected DocumentModel createDefaultCollections(final CoreSession session,
            DocumentModel userWorkspace) throws ClientException {
        DocumentModel doc = session.createDocumentModel(
                userWorkspace.getPath().toString(),
                CollectionConstants.DEFAULT_COLLECTIONS_NAME,
                CollectionConstants.COLLECTIONS_TYPE);
        doc.setProperty("dublincore", "title",
                CollectionConstants.DEFAULT_COLLECTIONS_NAME);
        doc.setProperty("dublincore", "description", "");
        doc = session.createDocument(doc);

        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false);
        ACE allowEverything = new ACE(session.getPrincipal().getName(),
                SecurityConstants.EVERYTHING, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowEverything, denyEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);

        return doc;
    }

}
