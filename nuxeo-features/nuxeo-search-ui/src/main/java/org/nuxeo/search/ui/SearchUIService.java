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
 *     Thomas Roger
 *     Nelson Silva
 */

package org.nuxeo.search.ui;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;

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
     * Returns the list of Content view headers associated to a search and depending of a local configuration.
     */
    List<ContentViewHeader> getContentViewHeaders(ActionContext actionContext, DocumentModel doc);

    /**
     * Save the current search in the user workspace with the given title.
     *
     * @param session the {@code CoreSession} to use
     * @param searchContentViewState the search to save
     * @param title the title of the being saved search
     * @return the saved search DocumentModel
     */
    DocumentModel saveSearch(CoreSession session, ContentViewState searchContentViewState, String title);

    /**
     * Returns the current user saved searches, located into its own user workspace.
     */
    List<DocumentModel> getCurrentUserSavedSearches(CoreSession session);

    /**
     * Returns all the accessible saved searches except the ones for the current user.
     */
    List<DocumentModel> getSharedSavedSearches(CoreSession session);

    /**
     * Load the content view state for a given saved search.
     *
     * @since 7.3
     */
    ContentViewState loadSearch(DocumentModel savedSearch);
}
