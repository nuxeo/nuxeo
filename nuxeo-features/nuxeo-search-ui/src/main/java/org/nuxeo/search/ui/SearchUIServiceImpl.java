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

import static org.nuxeo.search.ui.localconfiguration.Constants.SEARCH_CONFIGURATION_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.search.ui.localconfiguration.SearchConfiguration;

/**
 * @since 6.0
 */
public class SearchUIServiceImpl implements SearchUIService {

    private static Log log = LogFactory.getLog(SearchUIServiceImpl.class);

    public static final String SEARCH_CONTENT_VIEWS_CATEGORY = "SEARCH_CONTENT_VIEWS";

    public static final String CONTENT_VIEW_NAME_PROPERTY = "contentViewName";

    public static final String SAVED_SEARCHES_PROVIDER_NAME = "SAVED_SEARCHES";

    public static final String SHARED_SEARCHES_PROVIDER_NAME = "SHARED_SAVED_SEARCHES";

    @Override
    public List<ContentViewHeader> getContentViewHeaders(
            ActionContext actionContext) {
        return getContentViewHeaders(actionContext, null);
    }

    @Override
    public List<ContentViewHeader> getContentViewHeaders(
            ActionContext actionContext, DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        List<Action> actions = actionService.getActions(
                SEARCH_CONTENT_VIEWS_CATEGORY, actionContext);

        List<String> contentViewNames = new ArrayList<>();
        for (Action action : actions) {
            String contentViewName = (String) action.getProperties().get(
                    CONTENT_VIEW_NAME_PROPERTY);
            if (contentViewName != null) {
                contentViewNames.add(contentViewName);
            }
        }
        contentViewNames = filterContentViewNames(contentViewNames, doc);

        ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
        List<ContentViewHeader> contentViewHeaders = new ArrayList<>();
        for (String contentViewName : contentViewNames) {
            ContentViewHeader contentViewHeader = contentViewService.getContentViewHeader(contentViewName);
            if (contentViewHeader != null) {
                contentViewHeaders.add(contentViewHeader);
            }
        }
        return contentViewHeaders;
    }

    /**
     * Returns the filtered content view names based on the local configuration
     * if any.
     */
    protected List<String> filterContentViewNames(
            List<String> contentViewNames, DocumentModel currentDoc) {
        SearchConfiguration searchConfiguration = getSearchConfiguration(currentDoc);
        return searchConfiguration == null ? contentViewNames
                : searchConfiguration.filterAllowedContentViewNames(contentViewNames);
    }

    protected SearchConfiguration getSearchConfiguration(
            DocumentModel currentDoc) {
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        return localConfigurationService.getConfiguration(
                SearchConfiguration.class, SEARCH_CONFIGURATION_FACET,
                currentDoc);
    }

    public DocumentModel saveSearch(CoreSession session,
            ContentView searchContentView, String title) {
        UserWorkspaceService userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        DocumentModel uws = userWorkspaceService.getCurrentUserPersonalWorkspace(
                session, null);

        DocumentModel searchDoc = searchContentView.getSearchDocumentModel();
        searchDoc.setPropertyValue("cvd:contentViewName",
                searchContentView.getName());
        searchDoc.setPropertyValue("dc:title", title);
        PathSegmentService pathService = Framework.getLocalService(PathSegmentService.class);
        searchDoc.setPathInfo(uws.getPathAsString(),
                pathService.generatePathSegment(searchDoc));
        searchDoc = session.createDocument(searchDoc);
        session.save();

        return searchDoc;
    }

    public List<DocumentModel> getCurrentUserSavedSearches(CoreSession session)
            throws ClientException {
        return getDocuments(SAVED_SEARCHES_PROVIDER_NAME, session,
                session.getPrincipal().getName());
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getDocuments(String pageProviderName,
            CoreSession session, Object... parameters) throws ClientException {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("coreSession", (Serializable) session);
        return ((PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                pageProviderName, null, null, null, properties, parameters)).getCurrentPage();

    }

    public List<DocumentModel> getSharedSavedSearches(CoreSession session)
            throws ClientException {
        return getDocuments(SHARED_SEARCHES_PROVIDER_NAME, session,
                session.getPrincipal().getName());
    }

}
