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

import java.util.Locale;
import java.util.MissingResourceException;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.api.FavoritesConstants;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 5.9.4
 */
public class FavoritesManagerImpl extends DefaultComponent implements
        FavoritesManager {

    @Override
    public void addToFavorites(DocumentModel document, CoreSession session)
            throws ClientException {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        collectionManager.addToCollection(getFavorites(document, session),
                document, session);
    }

    @Override
    public boolean canAddToFavorites(DocumentModel document)
            throws ClientException {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        return collectionManager.isCollectable(document);
    }

    protected DocumentModel createFavorites(CoreSession session,
            DocumentModel userWorkspace) throws ClientException {
        DocumentModel doc = session.createDocumentModel(
                userWorkspace.getPath().toString(),
                FavoritesConstants.DEFAULT_FAVORITES_NAME,
                FavoritesConstants.FAVORITES_TYPE);
        String title = null;
        try {
            title = I18NUtils.getMessageString("messages",
                    FavoritesConstants.DEFAULT_FAVORITES_TITLE, new Object[0],
                    getLocale(session));
        } catch (MissingResourceException e) {
            title = FavoritesConstants.DEFAULT_FAVORITES_NAME;
        }
        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("dc:description", "");
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

    @Override
    public DocumentModel getFavorites(final DocumentModel context,
            final CoreSession session) throws ClientException {
        final UserWorkspaceService userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        final DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session, context);
        final DocumentRef lookupRef = new PathRef(
                userWorkspace.getPath().toString(),
                FavoritesConstants.DEFAULT_FAVORITES_NAME);
        if (session.exists(lookupRef)) {
            return session.getChild(userWorkspace.getRef(),
                    FavoritesConstants.DEFAULT_FAVORITES_NAME);
        } else {
            // does not exist yet, let's create it
            synchronized (this) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
                if (!session.exists(lookupRef)) {
                    boolean succeed = false;
                    try {
                        createFavorites(session, userWorkspace);
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

    protected Locale getLocale(final CoreSession session)
            throws ClientException {
        Locale locale = null;
        locale = Framework.getLocalService(LocaleProvider.class).getLocale(
                session);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return new Locale(Locale.getDefault().getLanguage());
    }

    @Override
    public boolean isFavorite(DocumentModel document, CoreSession session)
            throws ClientException {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        return collectionManager.isInCollection(
                getFavorites(document, session), document, session);
    }

    @Override
    public void removeFromFavorites(DocumentModel document, CoreSession session)
            throws ClientException {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        collectionManager.removeFromCollection(getFavorites(document, session),
                document, session);
    }

}
