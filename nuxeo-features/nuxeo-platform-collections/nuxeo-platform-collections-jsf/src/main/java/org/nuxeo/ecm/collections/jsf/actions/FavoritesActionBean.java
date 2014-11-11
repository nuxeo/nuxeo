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
package org.nuxeo.ecm.collections.jsf.actions;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.Messages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.4
 */
@Name("favoritesActions")
@Scope(ScopeType.STATELESS)
@BypassInterceptors
public class FavoritesActionBean {

    public void addCurrentDocumentToFavorites() throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getLocalService(FavoritesManager.class);
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            if (!favoritesManager.isFavorite(currentDocument, session)) {
                favoritesManager.addToFavorites(currentDocument, session);

                navigationContext.invalidateCurrentDocument();

                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

                final FacesMessages facesMessages = (FacesMessages) Component.getInstance(
                        "facesMessages", true);
                facesMessages.add(StatusMessage.Severity.INFO,
                        Messages.instance().get("favorites.addedToFavorites"));
            }

        }
    }

    public boolean canCurrentDocumentBeAddedToFavorites()
            throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getLocalService(FavoritesManager.class);
            final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            return collectionManager.isCollectable(currentDocument)
                    && !favoritesManager.isFavorite(currentDocument, session);
        }
        return false;
    }

    public boolean canCurrentDocumentBeRemovedFromFavorites()
            throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getLocalService(FavoritesManager.class);
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            return favoritesManager.isFavorite(currentDocument, session);
        }
        return false;
    }

    @Factory(value = "currentUserFavorites", scope = ScopeType.SESSION)
    public DocumentModel getCurrentFavorites() throws ClientException {
        FavoritesManager favoritesManager = Framework.getLocalService(FavoritesManager.class);
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final CoreSession session = (CoreSession) Component.getInstance(
                "documentManager", true);
        return favoritesManager.getFavorites(
                navigationContext.getCurrentDomain(), session);
    }

    public void removeCurrentDocumentFromFavorites() throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getLocalService(FavoritesManager.class);
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            if (favoritesManager.isFavorite(currentDocument, session)) {
                favoritesManager.removeFromFavorites(currentDocument, session);

                navigationContext.invalidateCurrentDocument();

                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

                final FacesMessages facesMessages = (FacesMessages) Component.getInstance(
                        "facesMessages", true);
                facesMessages.add(
                        StatusMessage.Severity.INFO,
                        Messages.instance().get(
                                "favorites.removedFromFavorites"));
            }

        }
    }

}
