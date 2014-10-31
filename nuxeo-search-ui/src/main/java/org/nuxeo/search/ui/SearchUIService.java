/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.search.ui;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;

/**
 * Service handling contributed searches and related saved searches.
 *
 * @since 6.0
 */
public interface SearchUIService {

    /**
     * Returns the list of Content view headers associated to a search.
     */
    List<ContentViewHeader> getContentViewHeaders(ActionContext actionContext);

    /**
     * Returns the list of Content view headers associated to a search and
     * depending of a local configuration.
     */
    List<ContentViewHeader> getContentViewHeaders(ActionContext actionContext,
            DocumentModel doc);

    /**
     * Save the current search in the user workspace with the given title.
     *
     * @param session the {@code CoreSession} to use
     * @param searchContentView the search to save
     * @param title the title of the being saved search
     * @return the saved search DocumentModel
     */
    DocumentModel saveSearch(CoreSession session,
            ContentView searchContentView, String title) throws ClientException;

    /**
     * Returns the current user saved searches, located into its own user
     * workspace.
     */
    List<DocumentModel> getCurrentUserSavedSearches(CoreSession session)
            throws ClientException;

    /**
     * Returns all the accessible saved searches except the ones for the current
     * user.
     */
    List<DocumentModel> getSharedSavedSearches(CoreSession session)
            throws ClientException;

}
