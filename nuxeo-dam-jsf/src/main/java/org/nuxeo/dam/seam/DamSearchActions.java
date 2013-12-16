/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.dam.seam;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.dam.DamConstants.DAM_MAIN_TAB_ACTION;
import static org.nuxeo.dam.DamConstants.REFRESH_DAM_SEARCH;
import static org.nuxeo.dam.DamConstants.SAVED_DAM_SEARCHES_PROVIDER_NAME;
import static org.nuxeo.dam.DamConstants.SHARED_DAM_SEARCHES_PROVIDER_NAME;
import static org.nuxeo.ecm.platform.contentview.jsf.ContentView.CONTENT_VIEW_PAGE_CHANGED_EVENT;
import static org.nuxeo.ecm.platform.contentview.jsf.ContentView.CONTENT_VIEW_PAGE_SIZE_CHANGED_EVENT;
import static org.nuxeo.ecm.platform.contentview.jsf.ContentView.CONTENT_VIEW_REFRESH_EVENT;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.json.JSONContentViewState;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.webapp.action.MainTabsActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles DAM search and permalinks related actions.
 *
 * @since 5.7
 */
@Name("damSearchActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamSearchActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DamSearchActions.class);

    public static final String SAVED_SEARCHES_LABEL = "label.dam.saved.searches";

    public static final String SHARED_SEARCHES_LABEL = "label.dam.shared.searches";

    public static final String SEARCH_FILTERS_LABEL = "label.dam.search.filters";

    public static final String SEARCH_SAVED_LABEL = "label.dam.search.saved";

    public static final String DAM_FLAG = "DAM";

    public static final String DAM_CODEC = "docpathdam";

    public static final String CONTENT_VIEW_NAME_PARAMETER = "contentViewName";

    public static final String CURRENT_PAGE_PARAMETER = "currentPage";

    public static final String PAGE_SIZE_PARAMETER = "pageSize";

    public static final String CONTENT_VIEW_STATE_PARAMETER = "state";

    @In(create = true)
    protected DamActions damActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected MainTabsActions mainTabsActions;

    @In(create = true)
    protected RestHelper restHelper;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected List<String> contentViewNames;

    protected Set<ContentViewHeader> contentViewHeaders;

    protected String currentContentViewName;

    protected String currentSelectedSavedSearchId;

    protected String currentPage;

    protected String pageSize;

    protected String savedSearchTitle;

    /**
     * @since 5.9.1
     */
    public String getJSONContentViewState() throws ClientException, UnsupportedEncodingException {
        ContentView contentView = contentViewActions.getContentView(currentContentViewName);
        ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
        ContentViewState state = contentViewService.saveContentView(contentView);
        return JSONContentViewState.toJSON(state, true);
    }

    public String getCurrentContentViewName() {
        if (currentContentViewName == null) {
            List<String> contentViewNames = getContentViewNames();
            if (!contentViewNames.isEmpty()) {
                currentContentViewName = contentViewNames.get(0);
            }
        }
        return currentContentViewName;
    }

    public void setCurrentContentViewName(String contentViewName)
            throws ClientException {
        this.currentContentViewName = contentViewName;
    }

    public String getCurrentSelectedSavedSearchId() {
        return currentSelectedSavedSearchId != null ? currentSelectedSavedSearchId
                : currentContentViewName;
    }

    public void setCurrentSelectedSavedSearchId(String selectedSavedSearchId)
            throws ClientException {
        if (contentViewNames.contains(selectedSavedSearchId)) {
            contentViewActions.reset(currentContentViewName);
            currentContentViewName = selectedSavedSearchId;
            currentSelectedSavedSearchId = null;
        } else {
            DocumentModel savedSearch = documentManager.getDocument(new IdRef(
                    selectedSavedSearchId));
            String contentViewName = (String) savedSearch.getPropertyValue("cvd:contentViewName");
            loadSavedSearch(contentViewName, savedSearch);
        }
    }

    public void loadSavedSearch(String contentViewName,
            DocumentModel searchDocument) throws ClientException {
        ContentView contentView = contentViewActions.getContentView(
                contentViewName, searchDocument);
        if (contentView != null) {
            currentContentViewName = contentViewName;
            currentSelectedSavedSearchId = searchDocument.getId();
        }
    }

    public List<String> getContentViewNames() {
        if (contentViewNames == null) {
            ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
            contentViewNames = new ArrayList<String>(
                    contentViewService.getContentViewNames(DAM_FLAG));
        }
        return contentViewNames;
    }

    public Set<ContentViewHeader> getContentViewHeaders()
            throws ClientException {
        if (contentViewHeaders == null) {
            contentViewHeaders = new HashSet<ContentViewHeader>();
            ContentViewService contentViewService = Framework.getLocalService(ContentViewService.class);
            for (String name : getContentViewNames()) {
                ContentViewHeader header = contentViewService.getContentViewHeader(name);
                if (header != null) {
                    contentViewHeaders.add(header);
                }
            }
        }
        return contentViewHeaders;
    }

    public void clearSearch() throws ClientException {
        contentViewActions.reset(getCurrentContentViewName());
        updateCurrentDocument();
    }

    @Observer(value = { REFRESH_DAM_SEARCH }, create = true)
    public void refreshAndRewind() throws ClientException {
        contentViewActions.refreshAndRewind(getCurrentContentViewName());
        updateCurrentDocument();
    }

    @SuppressWarnings("unchecked")
    public void updateCurrentDocument() throws ClientException {
        ContentView contentView = contentViewActions.getContentView(getCurrentContentViewName());
        updateCurrentDocument((PageProvider<DocumentModel>) contentView.getCurrentPageProvider());
    }

    public void updateCurrentDocument(PageProvider<DocumentModel> pageProvider)
            throws ClientException {
        if (pageProvider == null) {
            return;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        List<DocumentModel> docs = pageProvider.getCurrentPage();
        if (docs.isEmpty()) {
            // no document selected
            damActions.selectDocument(null);
        } else if (!docs.contains(currentDocument)) {
            damActions.selectDocument(docs.get(0));
        }
    }

    /*
     * ----- Load / Save searches -----
     */

    public List<SelectItem> getAllSavedSearchesSelectItems()
            throws ClientException {
        List<SelectItem> items = new ArrayList<SelectItem>();
        // Add saved searches
        SelectItemGroup userGroup = new SelectItemGroup(
                messages.get(SAVED_SEARCHES_LABEL));
        List<DocumentModel> userSavedSearches = getSavedSearches();
        List<SelectItem> userSavedSearchesItems = convertToSelectItems(userSavedSearches);
        userGroup.setSelectItems(userSavedSearchesItems.toArray(new SelectItem[userSavedSearchesItems.size()]));
        items.add(userGroup);
        // Add shared searches
        List<DocumentModel> otherUsersSavedFacetedSearches = getSharedSearches();
        List<SelectItem> otherUsersSavedSearchesItems = convertToSelectItems(otherUsersSavedFacetedSearches);
        SelectItemGroup allGroup = new SelectItemGroup(
                messages.get(SHARED_SEARCHES_LABEL));
        allGroup.setSelectItems(otherUsersSavedSearchesItems.toArray(new SelectItem[otherUsersSavedSearchesItems.size()]));
        items.add(allGroup);
        SelectItemGroup flaggedGroup = new SelectItemGroup(
                messages.get(SEARCH_FILTERS_LABEL));
        // Add flagged content views
        Set<ContentViewHeader> flaggedSavedSearches = getContentViewHeaders();
        List<SelectItem> flaggedSavedSearchesItems = convertCVToSelectItems(flaggedSavedSearches);
        flaggedGroup.setSelectItems(flaggedSavedSearchesItems.toArray(new SelectItem[flaggedSavedSearchesItems.size()]));
        items.add(flaggedGroup);
        return items;
    }

    protected List<DocumentModel> getSavedSearches() throws ClientException {
        return getDocuments(SAVED_DAM_SEARCHES_PROVIDER_NAME,
                documentManager.getPrincipal().getName());
    }

    protected List<DocumentModel> getSharedSearches() throws ClientException {
        return getDocuments(SHARED_DAM_SEARCHES_PROVIDER_NAME,
                documentManager.getPrincipal().getName());
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getDocuments(String pageProviderName,
            Object... parameters) throws ClientException {
        PageProviderService pageProviderService = Framework.getLocalService(PageProviderService.class);
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("coreSession", (Serializable) documentManager);
        return ((PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                pageProviderName, null, null, null, properties, parameters)).getCurrentPage();
    }

    protected List<SelectItem> convertToSelectItems(List<DocumentModel> docs)
            throws ClientException {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (DocumentModel doc : docs) {
            items.add(new SelectItem(doc.getId(), doc.getTitle(), ""));
        }
        return items;
    }

    protected List<SelectItem> convertCVToSelectItems(
            Set<ContentViewHeader> contentViewHeaders) {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (ContentViewHeader contentViewHeader : contentViewHeaders) {
            items.add(new SelectItem(contentViewHeader.getName(),
                    messages.get(contentViewHeader.getTitle()), ""));
        }
        return items;
    }

    public String getSavedSearchTitle() {
        return savedSearchTitle;
    }

    public void setSavedSearchTitle(String savedSearchTitle) {
        this.savedSearchTitle = savedSearchTitle;
    }

    public String saveSearch() throws ClientException {
        ContentView contentView = contentViewActions.getContentView(getCurrentContentViewName());
        if (contentView != null) {
            UserWorkspaceService userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
            DocumentModel uws = userWorkspaceService.getCurrentUserPersonalWorkspace(
                    documentManager, null);

            DocumentModel searchDoc = contentView.getSearchDocumentModel();
            searchDoc.setPropertyValue("cvd:contentViewName",
                    contentView.getName());
            searchDoc.setPropertyValue("dc:title", savedSearchTitle);
            PathSegmentService pathService = Framework.getLocalService(PathSegmentService.class);
            searchDoc.setPathInfo(uws.getPathAsString(),
                    pathService.generatePathSegment(searchDoc));
            searchDoc = documentManager.createDocument(searchDoc);
            documentManager.save();

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get(SEARCH_SAVED_LABEL));

            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    uws);

            savedSearchTitle = null;
            currentSelectedSavedSearchId = searchDoc.getId();
        }

        return null;
    }

    /*
     * ----- Permanent links -----
     */

    public void setState(String state) throws ClientException,
            UnsupportedEncodingException {
        if (StringUtils.isNotBlank(state)) {
            Long finalPageSize = null;
            if (!StringUtils.isBlank(pageSize)) {
                try {
                    finalPageSize = Long.valueOf(pageSize);
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "Unable to parse '%s' parameter with value '%s'",
                            PAGE_SIZE_PARAMETER, pageSize));
                }
            }

            Long finalCurrentPage = null;
            if (!StringUtils.isBlank(currentPage)) {
                try {
                    finalCurrentPage = Long.valueOf(currentPage);
                } catch (NumberFormatException e) {
                    log.warn(String.format(
                            "Unable to parse '%s' parameter with value '%s'",
                            CURRENT_PAGE_PARAMETER, currentPage));
                }
            }

            contentViewActions.restoreContentView(getCurrentContentViewName(),
                    finalCurrentPage, finalPageSize, null, state);
        }
        updateCurrentDocument();
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }


    /**
     * Compute a permanent link for the current search.
     */
    @SuppressWarnings("unchecked")
    public String getSearchPermanentLinkUrl() throws ClientException,
            UnsupportedEncodingException {
        // do not try to compute an URL if we don't have any CoreSession
        if (documentManager == null) {
            return null;
        }

        String currentContentViewName = getCurrentContentViewName();
        DocumentModel damCurrentDocument = mainTabsActions.getDocumentFor(DAM_MAIN_TAB_ACTION);
        DocumentView docView = computeDocumentView(damCurrentDocument);
        docView.setViewId("assets");
        docView.addParameter(CONTENT_VIEW_NAME_PARAMETER,
                currentContentViewName);
        docView.addParameter(CONTENT_VIEW_STATE_PARAMETER, getJSONContentViewState());
        DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
        String url = documentViewCodecManager.getUrlFromDocumentView(DAM_CODEC,
                docView, true, BaseURL.getBaseURL());
        return RestHelper.addCurrentConversationParameters(url);
    }

    protected DocumentView computeDocumentView(DocumentModel doc) {
        if (doc != null) {
            return new DocumentViewImpl(new DocumentLocationImpl(
                    documentManager.getRepositoryName(), new PathRef(
                            doc.getPathAsString())));
        } else {
            return new DocumentViewImpl(new DocumentLocationImpl(
                    documentManager.getRepositoryName(), null));
        }
    }

    public String getAssetPermanentLinkUrl() throws ClientException,
            UnsupportedEncodingException {
        // do not try to compute an URL if we don't have any CoreSession
        if (documentManager == null) {
            return null;
        }

        DocumentModel damCurrentDocument = mainTabsActions.getDocumentFor(DAM_MAIN_TAB_ACTION);
        DocumentView docView = computeDocumentView(damCurrentDocument);
        docView.setViewId("asset");
        DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
        return documentViewCodecManager.getUrlFromDocumentView(DAM_CODEC,
                docView, true, BaseURL.getBaseURL());
    }

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String loadPermanentLink(DocumentView docView)
            throws ClientException {
        restHelper.initContextFromRestRequest(docView);
        return "assets";
    }

    @Observer(value = { CONTENT_VIEW_PAGE_CHANGED_EVENT,
            CONTENT_VIEW_PAGE_SIZE_CHANGED_EVENT, CONTENT_VIEW_REFRESH_EVENT }, create = true)
    public void onContentViewPageProviderChanged(String contentViewName)
            throws ClientException {
        String currentContentViewName = getCurrentContentViewName();
        if (currentContentViewName != null
                && currentContentViewName.equals(contentViewName)
                && damActions.isOnDamView()) {
            updateCurrentDocument();
        }
    }

    /**
     * Reset attributes.
     *
     * @since 5.7.2
     */
    @Observer(value = { EventNames.FLUSH_EVENT }, create = false)
    @BypassInterceptors
    public void resetonFlush() {
        contentViewHeaders = null;
        contentViewNames = null;
        currentSelectedSavedSearchId = null;
        currentContentViewName = null;
    }

}
