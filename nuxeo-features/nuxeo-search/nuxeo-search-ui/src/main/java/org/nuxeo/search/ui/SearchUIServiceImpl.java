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

import static org.nuxeo.search.ui.localconfiguration.Constants.SEARCH_CONFIGURATION_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewStateImpl;
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

    public static final String CONTENT_VIEW_DISPLAY_FACET = "ContentViewDisplay";

    @Override
    public List<ContentViewHeader> getContentViewHeaders(ActionContext actionContext) {
        return getContentViewHeaders(actionContext, null);
    }

    @Override
    public List<ContentViewHeader> getContentViewHeaders(ActionContext actionContext, DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        List<Action> actions = actionService.getActions(SEARCH_CONTENT_VIEWS_CATEGORY, actionContext);

        List<String> contentViewNames = new ArrayList<>();
        for (Action action : actions) {
            String contentViewName = (String) action.getProperties().get(CONTENT_VIEW_NAME_PROPERTY);
            if (contentViewName != null) {
                contentViewNames.add(contentViewName);
            }
        }
        contentViewNames = filterContentViewNames(contentViewNames, doc);

        ContentViewService contentViewService = Framework.getService(ContentViewService.class);
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
     * Returns the filtered content view names based on the local configuration if any.
     */
    protected List<String> filterContentViewNames(List<String> contentViewNames, DocumentModel currentDoc) {
        SearchConfiguration searchConfiguration = getSearchConfiguration(currentDoc);
        return searchConfiguration == null ? contentViewNames
                : searchConfiguration.filterAllowedContentViewNames(contentViewNames);
    }

    protected SearchConfiguration getSearchConfiguration(DocumentModel currentDoc) {
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        return localConfigurationService.getConfiguration(SearchConfiguration.class, SEARCH_CONFIGURATION_FACET,
                currentDoc);
    }

    public DocumentModel saveSearch(CoreSession session, ContentViewState searchContentViewState, String title) {
        UserWorkspaceService userWorkspaceService = Framework.getService(UserWorkspaceService.class);
        DocumentModel uws = userWorkspaceService.getCurrentUserPersonalWorkspace(session, null);

        DocumentModel searchDoc = searchContentViewState.getSearchDocumentModel();
        DocumentRef ref = searchDoc.getRef();
        if (ref != null && session.exists(ref)) {
            // already a saved search, init a new doc
            DocumentModel bareDoc = DocumentModelFactory.createDocumentModel(searchDoc.getType());
            bareDoc.copyContent(searchDoc);
            searchDoc = bareDoc;
        }
        searchDoc.setPropertyValue("dc:title", title);

        if (searchDoc.hasFacet(CONTENT_VIEW_DISPLAY_FACET)) {
            searchDoc.setPropertyValue("cvd:contentViewName", searchContentViewState.getContentViewName());
            searchDoc.setPropertyValue("saved:providerName", searchContentViewState.getPageProviderName());
            searchDoc.setPropertyValue("saved:pageSize", searchContentViewState.getPageSize());
            searchContentViewState.getPageSize();
            List<SortInfo> sortInfos = searchContentViewState.getSortInfos();
            if (sortInfos != null) {
                ArrayList<Map<String, Serializable>> list = new ArrayList<>();
                String sortBy = "", sortOrder = "";
                for (SortInfo sortInfo : sortInfos) {
                    if (!sortBy.isEmpty()) {
                        sortBy += ",";
                        sortOrder += ",";
                    }
                    sortBy += sortInfo.getSortColumn();
                    sortOrder += sortInfo.getSortAscending() ? "ASC" : "DESC";
                    list.add(SortInfo.asMap(sortInfo));
                }
                searchDoc.setPropertyValue("cvd:sortInfos", list);
                searchDoc.setPropertyValue("saved:sortBy", sortBy);
                searchDoc.setPropertyValue("saved:sortOrder", sortOrder);
            }
            searchDoc.setPropertyValue("cvd:selectedLayoutColumns",
                    (Serializable) searchContentViewState.getResultColumns());
        } else {
            log.warn(String.format("Search document type %s is missing %s facet", searchDoc.getType(),
                    CONTENT_VIEW_DISPLAY_FACET));
        }

        PathSegmentService pathService = Framework.getService(PathSegmentService.class);
        searchDoc.setPathInfo(uws.getPathAsString(), pathService.generatePathSegment(searchDoc));
        searchDoc = session.createDocument(searchDoc);
        session.save();

        return searchDoc;
    }

    public List<DocumentModel> getCurrentUserSavedSearches(CoreSession session) {
        return getDocuments(SAVED_SEARCHES_PROVIDER_NAME, session, session.getPrincipal().getName());
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getDocuments(String pageProviderName, CoreSession session, Object... parameters) {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("coreSession", (Serializable) session);
        return ((PageProvider<DocumentModel>) pageProviderService.getPageProvider(pageProviderName, null, null, null,
                properties, parameters)).getCurrentPage();

    }

    public List<DocumentModel> getSharedSavedSearches(CoreSession session) {
        return getDocuments(SHARED_SEARCHES_PROVIDER_NAME, session, session.getPrincipal().getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public ContentViewState loadSearch(DocumentModel savedSearch) {
        if (!savedSearch.hasFacet(CONTENT_VIEW_DISPLAY_FACET)) {
            log.warn(String.format("Search document type %s is missing %s facet", savedSearch.getType(),
                    CONTENT_VIEW_DISPLAY_FACET));
            return null;
        }
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName((String) savedSearch.getPropertyValue("cvd:contentViewName"));
        state.setSearchDocumentModel(savedSearch);
        state.setSortInfos(getSortInfos(savedSearch));
        state.setResultColumns((List<String>) savedSearch.getPropertyValue("cvd:selectedLayoutColumns"));
        return state;
    }

    @SuppressWarnings("unchecked")
    List<SortInfo> getSortInfos(DocumentModel savedSearch) {
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) savedSearch.getPropertyValue(
                "cvd:sortInfos");
        List<SortInfo> sortInfos = new ArrayList<>();
        for (Map<String, Serializable> info : list) {
            sortInfos.add(SortInfo.asSortInfo(info));
        }
        return sortInfos;
    }
}
