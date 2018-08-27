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
package org.nuxeo.ecm.webapp.collections;

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

    public void addCurrentDocumentToFavorites() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getService(FavoritesManager.class);
            final CoreSession session = (CoreSession) Component.getInstance("documentManager", true);
            if (!favoritesManager.isFavorite(currentDocument, session)) {
                favoritesManager.addToFavorites(currentDocument, session);

                navigationContext.invalidateCurrentDocument();

                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

                final FacesMessages facesMessages = (FacesMessages) Component.getInstance("facesMessages", true);
                facesMessages.add(StatusMessage.Severity.INFO, Messages.instance().get("favorites.addedToFavorites"));
            }

        }
    }

    public boolean canCurrentDocumentBeAddedToFavorites() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getService(FavoritesManager.class);
            final CollectionManager collectionManager = Framework.getService(CollectionManager.class);
            final CoreSession session = (CoreSession) Component.getInstance("documentManager", true);
            return collectionManager.isCollectable(currentDocument)
                    && !favoritesManager.isFavorite(currentDocument, session);
        }
        return false;
    }

    public boolean canCurrentDocumentBeRemovedFromFavorites() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getService(FavoritesManager.class);
            final CoreSession session = (CoreSession) Component.getInstance("documentManager", true);
            return favoritesManager.isFavorite(currentDocument, session);
        }
        return false;
    }

    @Factory(value = "currentUserFavorites", scope = ScopeType.SESSION)
    public DocumentModel getCurrentFavorites() {
        FavoritesManager favoritesManager = Framework.getService(FavoritesManager.class);
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext", true);
        final CoreSession session = (CoreSession) Component.getInstance("documentManager", true);
        return favoritesManager.getFavorites(navigationContext.getCurrentDomain(), session);
    }

    public void removeCurrentDocumentFromFavorites() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance("navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final FavoritesManager favoritesManager = Framework.getService(FavoritesManager.class);
            final CoreSession session = (CoreSession) Component.getInstance("documentManager", true);
            if (favoritesManager.isFavorite(currentDocument, session)) {
                favoritesManager.removeFromFavorites(currentDocument, session);

                navigationContext.invalidateCurrentDocument();

                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

                final FacesMessages facesMessages = (FacesMessages) Component.getInstance("facesMessages", true);
                facesMessages.add(StatusMessage.Severity.INFO,
                        Messages.instance().get("favorites.removedFromFavorites"));
            }

        }
    }

}
