/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: SearchActionsBean.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.search;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.faces.application.FacesMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultSetImpl;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.SortNotSupportedException;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;

/**
 * Backing bean for search actions. Provides functions to perform a search based
 * on different query types, to retrieve results and handle selections.
 *
 * @author DM
 * @deprecated use {@link DocumentSearchActions} and content views instead
 */
@Deprecated
@Name("searchActions")
@Scope(ScopeType.CONVERSATION)
public class SearchActionsBean extends InputController implements
        SearchActions, Serializable {

    public static final String ACTION_PAGE_SEARCH_FORM = "search_form";

    public static final String ACTION_PAGE_SEARCH_RESULTS = "search_results";

    // will return to the same page
    public static final String ACTION_PAGE_SEARCH_NO_KEYWORDS = null;

    public static final String ACTION_PAGE_SEARCH_QUERY_ERROR = null;

    private static final long serialVersionUID = -4020792436254971735L;

    private static final Log log = LogFactory.getLog(SearchActionsBean.class);

    @Deprecated
    public static final String QM_ADVANCED = SearchActions.QM_ADVANCED;

    @Deprecated
    public static final String QM_SIMPLE = SearchActions.QM_SIMPLE;

    @Deprecated
    public static final String PROV_NXQL = SearchActions.PROV_NXQL;

    private static final String ACTION_PAGE_SEARCH_NXQL = "search_results_nxql";

    private static final String ACTION_PAGE_SEARCH_ADVANCED = "search_results_advanced";

    private static final String ACTION_PAGE_SEARCH_SIMPLE = "search_results_simple";

    @In(create = true)
    private transient SearchBusinessDelegate searchDelegate;

    @In(required = false)
    private transient Principal currentUser;

    @In(required = false, create = false)
    SearchResultsBean searchResults;

    // need to be required = false since this is accessed also before connecting
    // to a rep
    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(required = false, create = true)
    private transient DocumentsListsManager documentsListsManager;

    @In(required = false, create = true)
    private transient QueryModelActions queryModelActions;

    @In(required = false, create = true)
    private transient ResultsProvidersCache resultsProvidersCache;

    @In(required = false, create = true)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    private transient ClipboardActions clipboardActions;

    @In(create = true)
    private transient SearchColumns searchColumns;

    @In(create = true)
    private transient Context conversationContext;

    private String queryErrorMsg;

    // Exposed parameters

    private String nxql;

    private String simpleSearchKeywords = "";

    private String reindexPath = "";

    private SearchType searchTypeId = SearchType.KEYWORDS;

    // used for direct nxql only
    private static final int maxResultsCount = 10;

    public void init() {
        log.debug("Initializing...");
    }

    public void destroy() {
        log.debug("Destroy...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

    //
    // business methods
    //

    @BypassInterceptors
    public String getSimpleSearchKeywords() {
        return simpleSearchKeywords;
    }

    public void setSimpleSearchKeywords(String k) {
        simpleSearchKeywords = k;
    }

    public String getNxql() {
        return nxql;
    }

    public void setNxql(String k) {
        nxql = k;
    }

    @BypassInterceptors
    public String getSearchTypeId() {
        return searchTypeId.name();
    }

    public void setSearchTypeId(String type) {
        searchTypeId = SearchType.valueOf(type);
    }

    public String getReindexPath() {
        return reindexPath;
    }

    public void setReindexPath(String path) {
        reindexPath = path;
    }

    /**
     * A page can set searchType by simply specifying a 'searchType' request
     * parameter with value <code>KEYWORDS</code> or <code>NXQL</code>.
     */
    @RequestParameter("searchType")
    public void setSearchType(String type) {
        if (null == type) {
            // param not set
            return;
        }
        searchTypeId = SearchType.valueOf(type);
    }

    @Observer(value = EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED, create = false)
    @BypassInterceptors
    public void resetSearchField() {
        simpleSearchKeywords = "";
        searchTypeId = SearchType.KEYWORDS;
    }

    public String getQueryErrorMsg() {
        return queryErrorMsg;
    }

    public void setQueryErrorMsg(String msg) {
        queryErrorMsg = msg;
    }

    @Begin(join = true)
    public String search() {
        // clear the form...
        // TODO add this to CONVERSATION

        queryErrorMsg = "";
        // this.resultDocuments = null;

        return ACTION_PAGE_SEARCH_FORM;
    }

    public String getLatestNxql() {
        return (String) conversationContext.get("search.lastQuery");
    }

    /**
     * To be called from UI. Has actually only the effect of invalidating the
     * results providers. The search perform then relies on the provider farms
     * (including the present bean).
     */
    public String performSearch() {

        // notify search performed
        Events evtManager = Events.instance();
        log.debug("Fire Event: " + EventNames.SEARCH_PERFORMED);
        evtManager.raiseEvent(EventNames.SEARCH_PERFORMED);

        if (log.isDebugEnabled()) {
            log.debug("performing searchType: " + searchTypeId);
        }
        try {
            // XXX : hack !!!
            if (searchResults != null) {
                searchResults.reset();
            }

            String page;
            PagedDocumentsProvider resultsProvider;
            if (searchTypeId == SearchType.NXQL) {
                if (nxql == null) {
                    log.warn("Direct NXQL search: no nxql query "
                            + "has been provided");
                    return ACTION_PAGE_SEARCH_NO_KEYWORDS;
                }
                log.debug("Query: " + nxql);
                resultsProvidersCache.invalidate(PROV_NXQL);
                resultsProvider = resultsProvidersCache.get(PROV_NXQL);
                page = ACTION_PAGE_SEARCH_NXQL;
            } else if (searchTypeId == SearchType.FORM) {
                String sortColumn = searchColumns.getSortColumn();
                boolean sortAscending = searchColumns.getSortAscending();
                SortInfo sortInfo = null;
                if (sortColumn != null) {
                    sortInfo = new SortInfo(sortColumn, sortAscending);
                }

                resultsProvidersCache.invalidate(QM_ADVANCED);
                resultsProvider = resultsProvidersCache.get(QM_ADVANCED,
                        sortInfo);
                page = ACTION_PAGE_SEARCH_ADVANCED;
            } else if (searchTypeId == SearchType.KEYWORDS) {
                if (simpleSearchKeywords == null || simpleSearchKeywords == "") {
                    log.warn("simpleSearchKeywords not given");
                    facesMessages.add(FacesMessage.SEVERITY_INFO,
                            resourcesAccessor.getMessages().get(
                                    "feedback.search.noKeywords"));
                    return ACTION_PAGE_SEARCH_NO_KEYWORDS;
                }
                String[] keywords = simpleSearchKeywords.split(" ");
                for (String keyword : keywords) {
                    if (keyword.startsWith("*")) {
                        log.warn("Can't begin search with * character");
                        facesMessages.add(FacesMessage.SEVERITY_INFO,
                                resourcesAccessor.getMessages().get(
                                        "feedback.search.star"));
                        return ACTION_PAGE_SEARCH_NO_KEYWORDS;

                    }
                }
                resultsProvidersCache.invalidate(QM_SIMPLE);
                resultsProvider = resultsProvidersCache.get(QM_SIMPLE);
                page = ACTION_PAGE_SEARCH_SIMPLE;
            } else {
                throw new ClientException("Unknown search type: "
                        + searchTypeId);
            }

            if (resultsProvider instanceof SearchPageProvider) {
                String lastQuery = ((SearchPageProvider) resultsProvider).getQuery();
                conversationContext.set("search.lastQuery", lastQuery);
            } else {
                conversationContext.set("search.lastQuery", null);
            }

            return page;
        } catch (SortNotSupportedException e) {
            queryErrorMsg = e.getMessage();
            log.debug("Search error: " + e.getMessage(), e);
            return ACTION_PAGE_SEARCH_QUERY_ERROR;
        } catch (ClientException e) {
            // Present to user: TODO we should make the difference between
            // QueryException and actual errors.
            queryErrorMsg = e.getMessage();
            log.debug("Search error: " + e.getMessage(), e);
            return ACTION_PAGE_SEARCH_QUERY_ERROR;
        }
    }

    public List<DocumentModel> getResultDocuments(String providerName)
            throws ClientException {

        PagedDocumentsProvider provider = resultsProvidersCache.get(providerName);
        if (provider == null) {
            log.warn("resultsProvider not available for getResultDocuments");
            return new ArrayList<DocumentModel>();
        }
        return provider.getCurrentPage();
    }

    // User-friendly path.
    // GR TODO will actually uses the core connection (perfwise bad)
    public String getDocumentLocation(DocumentModel doc) {
        return searchDelegate.getDocLocation(doc);
    }

    // SelectModel to use in interface

    public SelectDataModel getResultsSelectModel(String providerName)
            throws ClientException {
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(SEARCH_DOCUMENT_LIST,
                getResultDocuments(providerName), selectedDocuments);
        model.addSelectModelListener(this);
        return model;
    }

    @WebRemote
    public String processSelectRow(String selectedDocRef, String providerName,
            Boolean selection) throws ClientException {
        DocumentModel data = null;
        List<DocumentModel> currentDocs = getResultDocuments(providerName);

        for (DocumentModel doc : currentDocs) {
            DocumentRef docRef = doc.getRef();
            // the search backend might have a bug filling the docref
            if (docRef == null) {
                log.error("null DocumentRef for doc: " + doc);
                continue;
            }
            if (docRef.reference().equals(selectedDocRef)) {
                data = doc;
                break;
            }
        }
        if (data == null) {
            return "ERROR : DataNotFound";
        }
        if (selection) {
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        } else {
            documentsListsManager.removeFromWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        }
        return computeSelectionActions();
    }

    private String computeSelectionActions() {
        List<Action> availableActions = clipboardActions.getActionsForSelection();
        List<String> availableActionIds = new ArrayList<String>();
        for (Action a : availableActions) {
            if (a.getAvailable()) {
                availableActionIds.add(a.getId());
            }
        }
        String res = "";
        if (!availableActionIds.isEmpty()) {
            res = StringUtils.join(availableActionIds.toArray(), "|");
        }
        return res;
    }

    // SelectModelListener interface

    public void processSelectRowEvent(SelectDataModelRowEvent event) {
        Boolean selection = event.getSelected();
        DocumentModel data = (DocumentModel) event.getRowData();
        if (selection) {
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        } else {
            documentsListsManager.removeFromWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        }
    }

    @Factory(value = "searchDocumentModel", scope = ScopeType.EVENT)
    public DocumentModel getDocumentModel() throws ClientException {
        return queryModelActions.get(QM_ADVANCED).getDocumentModel();
    }

    @Deprecated
    public void reindexDocuments() throws ClientException {
        reindexDocuments(reindexPath);
    }

    @Deprecated
    public void reindexDocuments(String path) throws ClientException {
        SearchService service = SearchServiceDelegate.getRemoteSearchService();

        // Reindex from path with fulltext
        if (documentManager != null) {
            try {
                service.reindexAll(documentManager.getRepositoryName(), path,
                        true);
            } catch (IndexingException e) {
                throw new ClientException(e);
            }
        } else {
            throw new ClientException(
                    "DocumentManager not found in Seam context...");
        }
    }

    public String reset() throws ClientException {
        queryModelActions.reset(QM_ADVANCED);
        return null;
    }

    /**
     * ResultsProviderFarm interface implementation.
     */
    public PagedDocumentsProvider getResultsProvider(String name)
            throws ClientException, ResultsProviderFarmUserException {
        // SQLQueryParser + QueryParseException
        return getResultsProvider(name, null);
    }

    public PagedDocumentsProvider getResultsProvider(String name,
            SortInfo sortInfo) throws ClientException,
            ResultsProviderFarmUserException {
        // TODO param!
        try {
            switch (searchTypeId) {
            case NXQL:
                if (sortInfo != null) {
                    throw new SortNotSupportedException();
                }
                ResultSet resultSet = new ResultSetImpl(nxql, documentManager,
                        0, maxResultsCount,
                        Collections.<ResultItem> emptyList(), 0, 0).replay();
                SearchPageProvider nxqlProvider = new SearchPageProvider(
                        resultSet, false, null, nxql);
                nxqlProvider.setName(name);
                return nxqlProvider;
            case KEYWORDS:
                Object[] sK = { simpleSearchKeywords };
                QueryModel qm = queryModelActions.get(QM_SIMPLE);
                PagedDocumentsProvider simpleProvider = qm.getResultsProvider(
                        documentManager, sK, sortInfo);
                simpleProvider.setName(name);
                return simpleProvider;
            default:
                throw new ClientException("UNknown search type");
            }
        } catch (SearchException e) {
            throw new ClientException("Error while performing search", e);
        } catch (QueryParseException e) {
            throw new ResultsProviderFarmUserException(
                    "label.search.service.wrong.query", e);
        }
    }

    @Observer(value = { EventNames.DOCUMENT_CHILDREN_CHANGED }, create = false)
    public void refreshCache() {
        // XXX invalidate both because no way to know in which list it appended
        resultsProvidersCache.invalidate(QM_SIMPLE);
        resultsProvidersCache.invalidate(QM_ADVANCED);
        resultsProvidersCache.invalidate(PROV_NXQL);
    }

    public boolean isReindexingAll() {
        SearchService service = SearchServiceDelegate.getRemoteSearchService();
        return service.isReindexingAll();
    }

}
