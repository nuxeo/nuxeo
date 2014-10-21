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

package org.nuxeo.search.ui.seam;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.webapp.helpers.EventNames.LOCAL_CONFIGURATION_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewStateImpl;
import org.nuxeo.ecm.platform.contentview.json.JSONContentViewState;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.webapp.action.ActionContextProvider;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.search.ui.SearchUIService;

/**
 * Seam bean handling Search main tab actions.
 *
 * @since 5.9.6
 */
@Name("searchUIActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class SearchUIActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = getLog(SearchUIActions.class);

    public static final String SAVED_SEARCHES_LABEL = "label.saved.searches";

    public static final String SHARED_SEARCHES_LABEL = "label.shared.searches";

    public static final String SEARCH_FILTERS_LABEL = "label.search.filters";

    public static final String SEARCH_SAVED_LABEL = "label.search.saved";

    public static final String MAIN_TABS_SEARCH = "MAIN_TABS:search";

    public static final String SEARCH_VIEW_ID = "/search/search.xhtml";

    public static final String SEARCH_CODEC = "docpathsearch";

    public static final String SIMPLE_SEARCH_CONTENT_VIEW_NAME = "simple_search";

    public static final String NXQL_SEARCH_CONTENT_VIEW_NAME = "nxql_search";

    public static final String DEFAULT_NXQL_QUERY = "SELECT * FROM Document" +
            " WHERE ecm:mixinType != 'HiddenInNavigation'" +
            " AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0" +
            " AND ecm:currentLifeCycleState != 'deleted'";

    public static final String CONTENT_VIEW_NAME_PARAMETER = "contentViewName";

    public static final String CURRENT_PAGE_PARAMETER = "currentPage";

    public static final String PAGE_SIZE_PARAMETER = "pageSize";

    public static final String CONTENT_VIEW_STATE_PARAMETER = "state";

    public static final String SEARCH_TERM_PARAMETER = "searchTerm";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected RestHelper restHelper;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected ContentViewService contentViewService;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String simpleSearchKeywords = "";

    protected String nxqlQuery = DEFAULT_NXQL_QUERY;

    protected List<ContentViewHeader> contentViewHeaders;

    protected String currentContentViewName;

    protected String currentSelectedSavedSearchId;

    protected String currentPage;

    protected String pageSize;

    protected String searchTerm;

    protected String savedSearchTitle;

    public String getSearchMainTab() {
        return MAIN_TABS_SEARCH;
    }

    public void setSearchMainTab(String tabs) {
        webActions.setCurrentTabIds(!StringUtils.isBlank(tabs) ? tabs
                : MAIN_TABS_SEARCH);
    }

    public String getSearchViewTitle() {
        if (currentSelectedSavedSearchId != null) {
            DocumentModel savedSearch = documentManager.getDocument(new IdRef(
                    currentSelectedSavedSearchId));
            return savedSearch.getTitle();
        } else if (currentContentViewName != null) {
            ContentView cv = contentViewActions.getContentView(currentContentViewName);
            return cv.getTranslateTitle() ? messages.get(cv.getTitle()) : cv.getTitle();
        }
        return null;
    }

    /**
     * Returns true if the user is viewing SEARCH.
     */
    public boolean isOnSearchView() {
        if (FacesContext.getCurrentInstance() == null) {
            return false;
        }

        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        if (viewRoot != null) {
            String viewId = viewRoot.getViewId();
            // FIXME find a better way to update the current document only
            // if we are on SEARCH
            if (SEARCH_VIEW_ID.equals(viewId)) {
                return true;
            }
        }
        return false;
    }

    public String getJSONContentViewState() throws ClientException,
            UnsupportedEncodingException {
        ContentView contentView = contentViewActions.getContentView(currentContentViewName);
        ContentViewService contentViewService = Framework.getService(ContentViewService.class);
        ContentViewState state = contentViewService.saveContentView(contentView);
        return JSONContentViewState.toJSON(state, true);
    }

    public String getCurrentContentViewName() {
        if (currentContentViewName == null) {
            List<ContentViewHeader> contentViewHeaders = getContentViewHeaders();
            if (!contentViewHeaders.isEmpty()) {
                currentContentViewName = contentViewHeaders.get(0).getName();
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
        for (ContentViewHeader contentViewHeader : contentViewHeaders) {
            if (contentViewHeader.getName().equals(selectedSavedSearchId)) {
                contentViewActions.reset(currentContentViewName);
                currentContentViewName = selectedSavedSearchId;
                currentSelectedSavedSearchId = null;
                return;
            }
        }
        DocumentModel savedSearch = documentManager.getDocument(new IdRef(
                selectedSavedSearchId));
        String contentViewName = (String) savedSearch.getPropertyValue("cvd:contentViewName");
        loadSavedSearch(contentViewName, savedSearch);
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

    public List<ContentViewHeader> getContentViewHeaders()
            throws ClientException {
        if (contentViewHeaders == null) {
            SearchUIService searchUIService = Framework.getService(SearchUIService.class);
            contentViewHeaders = searchUIService.getContentViewHeaders(
                    actionContextProvider.createActionContext(),
                    navigationContext.getCurrentDocument());
        }
        return contentViewHeaders;
    }

    public void clearSearch() throws ClientException {
        contentViewActions.reset(getCurrentContentViewName());
    }

    public void refreshAndRewind() throws ClientException {
        contentViewActions.refreshAndRewind(getCurrentContentViewName());
    }

    public void refreshAndRewindAndResetAggregates() throws ClientException {
        contentViewActions.resetAggregates(getCurrentContentViewName());
        contentViewActions.refreshAndRewind(getCurrentContentViewName());
    }

    /*
     * ----- Load / Save searches -----
     */

    public List<SelectItem> getAllSavedSearchesSelectItems()
            throws ClientException {
        List<SelectItem> items = new ArrayList<>();

        // Add flagged content views
        SelectItemGroup flaggedGroup = new SelectItemGroup(
                messages.get(SEARCH_FILTERS_LABEL));
        List<ContentViewHeader> flaggedSavedSearches = getContentViewHeaders();
        List<SelectItem> flaggedSavedSearchesItems = convertCVToSelectItems(flaggedSavedSearches);
        flaggedGroup.setSelectItems(flaggedSavedSearchesItems.toArray(new SelectItem[flaggedSavedSearchesItems.size()]));
        items.add(flaggedGroup);

        // Add saved searches
        List<DocumentModel> userSavedSearches = getSavedSearches();
        if (!userSavedSearches.isEmpty()) {
            SelectItemGroup userGroup = new SelectItemGroup(
                    messages.get(SAVED_SEARCHES_LABEL));

            List<SelectItem> userSavedSearchesItems = convertToSelectItems(userSavedSearches);
            userGroup.setSelectItems(userSavedSearchesItems.toArray(new SelectItem[userSavedSearchesItems.size()]));
            items.add(userGroup);
        }

        // Add shared searches
        List<DocumentModel> otherUsersSavedFacetedSearches = getSharedSearches();
        if (!otherUsersSavedFacetedSearches.isEmpty()) {
            List<SelectItem> otherUsersSavedSearchesItems = convertToSelectItems(otherUsersSavedFacetedSearches);
            SelectItemGroup allGroup = new SelectItemGroup(
                    messages.get(SHARED_SEARCHES_LABEL));
            allGroup.setSelectItems(otherUsersSavedSearchesItems.toArray(new SelectItem[otherUsersSavedSearchesItems.size()]));
            items.add(allGroup);
        }
        return items;
    }

    protected List<DocumentModel> getSavedSearches() throws ClientException {
        SearchUIService searchUIService = Framework.getService(SearchUIService.class);
        return searchUIService.getCurrentUserSavedSearches(documentManager);
    }

    protected List<DocumentModel> getSharedSearches() throws ClientException {
        SearchUIService searchUIService = Framework.getService(SearchUIService.class);
        return searchUIService.getSharedSavedSearches(documentManager);
    }

    protected List<SelectItem> convertToSelectItems(List<DocumentModel> docs)
            throws ClientException {
        List<SelectItem> items = new ArrayList<>();
        for (DocumentModel doc : docs) {
            items.add(new SelectItem(doc.getId(), doc.getTitle(), ""));
        }
        return items;
    }

    protected List<SelectItem> convertCVToSelectItems(
            List<ContentViewHeader> contentViewHeaders) {
        List<SelectItem> items = new ArrayList<>();
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
            SearchUIService searchUIService = Framework.getService(SearchUIService.class);
            DocumentModel savedSearch = searchUIService.saveSearch(
                    documentManager, contentView, savedSearchTitle);
            currentSelectedSavedSearchId = savedSearch.getId();

            savedSearchTitle = null;
            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get(SEARCH_SAVED_LABEL));

            // Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
            // uws);

        }
        return null;
    }

    public void cancelSaveSearch() {
        savedSearchTitle = null;
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

    public void setSearchTerm(String searchTerm) throws ClientException,
            UnsupportedEncodingException {
        // If the search term is not defined, we don't do the logic
        if (!StringUtils.isEmpty(searchTerm)) {
            // By default, the "simple_search" content view is used
            currentContentViewName = SIMPLE_SEARCH_CONTENT_VIEW_NAME;
            // Create a ContentViewState
            ContentView cv = contentViewService.getContentView(SIMPLE_SEARCH_CONTENT_VIEW_NAME);
            DocumentModel searchDocumentModel = cv.getSearchDocumentModel();
            // set the search term
            searchDocumentModel.setPropertyValue("default_search:ecm_fulltext",
                    searchTerm);
            ContentViewState state = new ContentViewStateImpl();
            state.setSearchDocumentModel(searchDocumentModel);
            state.setContentViewName(getCurrentContentViewName());
            contentViewActions.restoreContentView(state);
        }
    }

    /**
     * Compute a permanent link for the current search.
     */
    public String getSearchPermanentLinkUrl() throws ClientException,
            UnsupportedEncodingException {
        // do not try to compute an URL if we don't have any CoreSession
        if (documentManager == null) {
            return null;
        }

        return generateSearchUrl(true);
    }

    /**
     * @return the URL of the search tab with the search term defined.
     */
    public String getSearchTabUrl(String searchTerm) throws ClientException,
            UnsupportedEncodingException {
        // do not try to compute an URL if we don't have any CoreSession
        if (documentManager == null) {
            return null;
        }
        // Set the value of the searched term
        this.searchTerm = searchTerm;

        return generateSearchUrl(false);
    }

    /**
     * Create the url to access the Search tab.
     *
     * @param withState If set to true, the state is added in the parameters.
     */
    protected String generateSearchUrl(boolean withState)
            throws ClientException, UnsupportedEncodingException {
        String currentContentViewName = getCurrentContentViewName();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentView docView = computeDocumentView(currentDocument);
        docView.setViewId("search");
        docView.addParameter(CONTENT_VIEW_NAME_PARAMETER,
                currentContentViewName);
        // Add the state if needed
        if (withState) {
            docView.addParameter(CONTENT_VIEW_STATE_PARAMETER,
                    getJSONContentViewState());
        }

        DocumentViewCodecManager documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        String url = documentViewCodecManager.getUrlFromDocumentView(
                SEARCH_CODEC, docView, true, BaseURL.getBaseURL());

        return RestHelper.addCurrentConversationParameters(url);
    }

    protected DocumentView computeDocumentView(DocumentModel doc) {
        return new DocumentViewImpl(new DocumentLocationImpl(
                documentManager.getRepositoryName(), doc != null ? new PathRef(
                        doc.getPathAsString()) : null));
    }

    /*
     * ----- Simple Search -----
     */
    public String getSimpleSearchKeywords() {
        return simpleSearchKeywords;
    }

    public void setSimpleSearchKeywords(String simpleSearchKeywords) {
        this.simpleSearchKeywords = simpleSearchKeywords;
    }

    public void validateSimpleSearchKeywords(FacesContext context,
                                             UIComponent component, Object value) {
        if (!(value instanceof String)
                || StringUtils.isEmpty(((String) value).trim())) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context, "feedback.search.noKeywords"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
        String[] keywords = ((String) value).trim().split(" ");
        for (String keyword : keywords) {
            if (keyword.startsWith("*")) {
                // Can't begin search with * character
                FacesMessage message = new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                        context, "feedback.search.star"), null);
                // also add global message
                context.addMessage(null, message);
                throw new ValidatorException(message);
            }
        }
    }

    public String doSimpleSearch() {
        setSearchMainTab(null);
        currentContentViewName = SIMPLE_SEARCH_CONTENT_VIEW_NAME;
        ContentView contentView = contentViewActions.getContentView(SIMPLE_SEARCH_CONTENT_VIEW_NAME);
        DocumentModel searchDoc = contentView.getSearchDocumentModel();
        searchDoc.setPropertyValue("defaults:ecm_fulltext", simpleSearchKeywords);
        refreshAndRewind();
        return "search";
    }

    /*
     * ----- NXQL Search -----
     */
    public String getNxqlQuery() {
        return nxqlQuery;
    }

    public void setNxqlQuery(String nxqlQuery) {
        this.nxqlQuery = nxqlQuery;
    }

    public boolean isNxqlSearchSelected() {
        return NXQL_SEARCH_CONTENT_VIEW_NAME.equals(currentContentViewName);
    }

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String loadPermanentLink(DocumentView docView)
            throws ClientException {
        restHelper.initContextFromRestRequest(docView);
        return "search";
    }

    @Observer(value = LOCAL_CONFIGURATION_CHANGED)
    public void invalidateContentViewsName() {
        clearSearch();
        contentViewHeaders = null;
        currentContentViewName = null;
    }

    @Observer(value = USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED)
    public void invalidateContentViewsNameIfChanged() throws ClientException {
        List<ContentViewHeader> temp = new ArrayList<>(
                Framework.getLocalService(SearchUIService.class).getContentViewHeaders(
                        actionContextProvider.createActionContext(),
                        navigationContext.getCurrentDocument()));
        if (!temp.isEmpty()) {
            String s = temp.get(0).getName();
            if (s != null && !s.equals(currentContentViewName)) {
                invalidateContentViewsName();
            }
        }
    }

    /**
     * Reset attributes.
     */
    @Observer(value = { EventNames.FLUSH_EVENT }, create = false)
    @BypassInterceptors
    public void resetOnFlush() {
        contentViewHeaders = null;
        currentSelectedSavedSearchId = null;
        currentContentViewName = null;
        nxqlQuery = DEFAULT_NXQL_QUERY;
        simpleSearchKeywords = "";
    }

    public String getSearchTermParameter() {
        return SEARCH_TERM_PARAMETER;
    }
}
