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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.search.ui.localconfiguration.SearchConfiguration;

/**
 * @since 5.9.6
 */
public class SearchUIServiceImpl implements SearchUIService {

    private static Log log = LogFactory.getLog(SearchUIServiceImpl.class);

    public static final String SEARCH_CONTENT_VIEW_FLAG = "SEARCH";

    public static final String SAVED_SEARCHES_PROVIDER_NAME = "SAVED_SEARCHES";

    public static final String SHARED_SEARCHES_PROVIDER_NAME = "SHARED_SAVED_SEARCHES";

    public Set<String> getContentViewNames() throws ClientException {
        return getContentViewNames(null);
    }

    public Set<String> getContentViewNames(DocumentModel currentDoc)
            throws ClientException {
        ContentViewService contentViewService = Framework.getService(ContentViewService.class);
        return filterContentViewNames(
                contentViewService.getContentViewNames(SEARCH_CONTENT_VIEW_FLAG),
                currentDoc);
    }

    /**
     * Returns the filtered content view names based on the local configuration
     * if any.
     */
    protected Set<String> filterContentViewNames(Set<String> contentViewNames,
            DocumentModel currentDoc) {
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
            ContentView searchContentView, String title) throws ClientException {
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
