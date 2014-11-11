/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.search;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.types.FieldWidget;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.runtime.api.Framework;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @deprecated use {@link DocumentSearchActions} and content views instead
 */
@Deprecated
@Name("searchResults")
@Scope(ScopeType.CONVERSATION)
public class SearchResultsBean extends InputController implements
        SearchResults, Serializable {

    private static final long serialVersionUID = 7823660685121811606L;

    private static final Log log = LogFactory.getLog(SearchResultsBean.class);

    public static final String SEARCH_DOCUMENT_LIST = "SEARCH_DOCUMENT_LIST";

    @In(required = false, create = true)
    protected transient SearchColumns searchColumns;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    protected String providerName;

    @RequestParameter("providerName")
    protected String newProviderName;

    @RequestParameter("sortColumn")
    protected String newSortColumn;

    @In(required = false, create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @In(create = true)
    protected transient ClipboardActions clipboardActions;

    // Should never be access for read directly
    protected transient PagedDocumentsProvider provider;

    public void reset() {
        provider = null;
    }

    public void init() {
        log.debug("Initializing...");
    }

    public void destroy() {
        log.debug("Destroy...");
    }

    public String repeatSearch() throws ClientException {
        if (newProviderName == null) {
            throw new ClientException("providerName not set");
        }
        String sortColumn = null;
        boolean sortAscending = true;

        SortInfo sortInfo = getProvider(newProviderName).getSortInfo();
        if (sortInfo != null) {
            sortColumn = sortInfo.getSortColumn();
            sortAscending = sortInfo.getSortAscending();
        }

        if (StringUtils.equals(sortColumn, newSortColumn)) {
            sortAscending = !sortAscending;
        } else {
            sortColumn = newSortColumn;
            sortAscending = true;
        }
        sortInfo = new SortInfo(sortColumn, sortAscending);

        resultsProvidersCache.invalidate(providerName);
        provider = resultsProvidersCache.get(providerName, sortInfo);

        return null;
    }

    public PagedDocumentsProvider getProvider() throws ClientException {
        if (providerName == null) {
            throw new ClientException("No provider name has been specified yet");
        }
        return getProvider(providerName);
    }

    /**
     * Has the effect of setting the <code>providerName</code> field.
     */
    public PagedDocumentsProvider getProvider(String providerName)
            throws ClientException {
        provider = resultsProvidersCache.get(providerName);
        if (provider == null) {
            throw new ClientException(
                    "Unknown or unbuildable results provider: " + providerName);
        }
        return provider;
    }

    public String getSortColumn() throws ClientException {
        SortInfo sortInfo = getProvider().getSortInfo();
        return sortInfo == null ? null : sortInfo.getSortColumn();
    }

    public boolean isSortAscending() throws ClientException {
        SortInfo sortInfo = getProvider().getSortInfo();
        return sortInfo == null ? true : sortInfo.getSortAscending();
    }

    public List<DocumentModel> getResultDocuments(String providerName)
            throws ClientException {
        return getProvider(providerName).getCurrentPage();
    }

    // SelectModels to use in interface
    @Factory(value = "searchSelectModel_advanced", scope = ScopeType.EVENT)
    public SelectDataModel getResultsSelectModelAdvanced()
            throws ClientException {
        return getResultsSelectModel(SearchActionsBean.QM_ADVANCED);
    }

    @Factory(value = "searchSelectModel_nxql", scope = ScopeType.EVENT)
    public SelectDataModel getResultsSelectModelNxql() throws ClientException {
        return getResultsSelectModel(SearchActionsBean.PROV_NXQL);
    }

    @Factory(value = "searchSelectModel_simple", scope = ScopeType.EVENT)
    public SelectDataModel getResultsSelectModelSimple() throws ClientException {
        return getResultsSelectModel(SearchActionsBean.QM_SIMPLE);
    }

    // GR TODO use a provider invalidation event
    @Observer(value = { EventNames.DOCUMENT_CHILDREN_CHANGED }, create = false)
    @BypassInterceptors
    public void refreshSelectModels() {
        Context context = Contexts.getEventContext();
        context.remove("searchSelectModel_simple");
        context.remove("searchSelectModel_nxql");
        context.remove("searchSelectModel_advanced");
    }

    public SelectDataModel getResultsSelectModel(String providerName)
            throws ClientException {
        if (providerName == null) {
            throw new ClientException("providerName has not been set yet");
        }
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
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

    public boolean isSortable() throws ClientException {
        return getProvider().isSortable();
    }

    public String downloadCSV() throws ClientException {
        try {
            if (newProviderName == null) {
                throw new ClientException("providerName not set");
            }
            PagedDocumentsProvider provider = getProvider(newProviderName);
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"search_results.csv\"");

            char separator = Framework.getProperty(
                    "org.nuxeo.ecm.webapp.search.csv.separator", ",").charAt(0);
            char quotechar = Framework.getProperty(
                    "org.nuxeo.ecm.webapp.search.csv.quotechar", "\"").charAt(0);
            String endOfLine = Framework.getProperty(
                    "org.nuxeo.ecm.webapp.search.csv.endofline", "\n");
            CSVWriter writer = new CSVWriter(response.getWriter(), separator,
                    quotechar, endOfLine);

            List<FieldWidget> widgetList = searchColumns.getResultColumns();
            Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);

            String[] columnNames = new String[widgetList.size()];
            int i = 0;
            for (FieldWidget widget : widgetList) {
                String columnName = resourcesAccessor.getMessages().get(
                        widget.getLabel());
                columnNames[i++] = columnName;
            }
            writer.writeNext(columnNames);

            // GR dump all pages... why not, but we need to restore current
            // page
            // number.
            int currentPage = provider.getCurrentPageIndex();
            int pageCount = provider.getNumberOfPages();
            for (int page = 0; page < pageCount; page++) {
                DocumentModelList docModelList = provider.getPage(page);
                for (DocumentModel docModel : docModelList) {
                    String[] columns = new String[widgetList.size()];
                    i = 0;
                    for (FieldWidget widget : widgetList) {
                        String fieldSchema = widget.getSchemaName();
                        String fieldName = widget.getFieldName();
                        Object value;

                        if (fieldSchema.equals("dublincore")
                                && fieldName.equals("title")) {
                            value = DocumentModelFunctions.titleOrId(docModel);
                        } else if (fieldSchema.equals("ecm")
                                && fieldName.equals("primaryType")) {
                            value = docModel.getType();
                        } else if (fieldSchema.equals("ecm")
                                && fieldName.equals("currentLifeCycleState")) {
                            value = docModel.getCurrentLifeCycleState();
                        } else {
                            value = docModel.getProperty(fieldSchema, fieldName);
                        }

                        String stringValue;
                        if (value == null) {
                            stringValue = "";
                        } else if (value instanceof GregorianCalendar) {
                            GregorianCalendar gValue = (GregorianCalendar) value;
                            stringValue = df.format(gValue.getTime());
                        } else if (value instanceof Object[]) {
                            stringValue = StringUtils.join(
                                    Arrays.asList((Object[]) value), ", ");
                        } else {
                            stringValue = String.valueOf(value);
                        }
                        columns[i++] = stringValue;
                    }
                    writer.writeNext(columns);
                }
            }
            writer.close();
            response.flushBuffer();
            FacesContext.getCurrentInstance().responseComplete();
            // restoring current page
            provider.getPage(currentPage);
        } catch (IOException e) {
            throw new ClientException("download csv failed", e);
        }
        return null;
    }

}
