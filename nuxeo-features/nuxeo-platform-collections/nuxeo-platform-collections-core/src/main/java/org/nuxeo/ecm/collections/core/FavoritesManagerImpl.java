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

import java.util.Locale;
import java.util.MissingResourceException;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.api.FavoritesConstants;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    protected DocumentModel createFavorites(CoreSession session, DocumentModel userWorkspace) {
        DocumentModel doc = session.createDocumentModel(userWorkspace.getPath().toString(),
                FavoritesConstants.DEFAULT_FAVORITES_NAME, FavoritesConstants.FAVORITES_TYPE);
        String title = null;
        try {
            title = I18NUtils.getMessageString("messages", FavoritesConstants.DEFAULT_FAVORITES_TITLE, new Object[0],
                    getLocale(session));
        } catch (MissingResourceException e) {
            title = FavoritesConstants.DEFAULT_FAVORITES_NAME;
        }
        doc.setPropertyValue("dc:title", title);
        doc.setPropertyValue("dc:description", "");
        return doc;
    }

    protected DocumentModel initCreateFavorites(CoreSession session, DocumentModel favorites) {
        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        ACE allowEverything = new ACE(session.getPrincipal().getName(), SecurityConstants.EVERYTHING, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowEverything, denyEverything });
        acp.addACL(acl);
        favorites.setACP(acp, true);
        return favorites;
    }

    @Override
    public DocumentModel getFavorites(final DocumentModel context, final CoreSession session) {
        final UserWorkspaceService userWorkspaceService = Framework.getService(UserWorkspaceService.class);
        final DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(session, context);
        if (userWorkspace == null) {
            // no user workspace => no favorites (transient user for instance)
            return null;
        }
        DocumentModel favorites = createFavorites(session, userWorkspace);
        return session.getOrCreateDocument(favorites, doc -> initCreateFavorites(session, doc));
    }

    protected Locale getLocale(final CoreSession session) {
        Locale locale = null;
        locale = Framework.getService(LocaleProvider.class).getLocale(session);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return new Locale(Locale.getDefault().getLanguage());
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
