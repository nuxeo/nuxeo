/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query.jsf;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.smart.query.HistoryList;
import org.nuxeo.ecm.platform.smart.query.SmartQuery;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.nuxeo.search.ui.seam.SearchUIActions;

/**
 * Seam component handling a {@link IncrementalSmartNXQLQuery} instance created for a given existing query string.
 * <p>
 * Also handles undo/redo actions and all ajax interactions for an incremental query build page.
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
@Name("smartNXQLQueryActions")
@Scope(ScopeType.PAGE)
public class SmartNXQLQueryActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int HISTORY_CAPACITY = 20;

    /**
     * @deprecated since 8.1: query part held/set directly on the content view search document model
     */
    protected String queryPart;

    protected HistoryList<String> queryPartHistory;

    protected HistoryList<String> redoQueryPartHistory;

    protected IncrementalSmartNXQLQuery currentSmartQuery;

    /**
     * @deprecated since 8.1: selected columns directly held/set on the content view search document model
     */
    @Deprecated
    protected List<String> selectedLayoutColumns;

    /**
     * @deprecated since 8.1: search sort infos directly held/set on the content view search document model
     */
    @Deprecated
    protected List<SortInfo> searchSortInfos;

    /**
     * Request parameter passed from the widget that makes it possible to decide whether the backing object should be
     * updated when performing ajax calls.
     * <p>
     * For instance, on the smart search form, any ajax action should update the backing property {@link #queryPart}.
     * When the query part is held by a document property, it should not be updated on ajax actions: only the global
     * submit of the form should impact it.
     *
     * @deprecated since 8.1: query part is now held by the content view search document model, consider it does not
     *             need to be updated until user clicks on "filter".
     */
    @RequestParameter
    @Deprecated
    protected Boolean updateQueryPart;

    /**
     * Request parameter making it possible to find the component holding the query part to update it.
     */
    @RequestParameter
    protected String queryPartComponentId;

    /**
     * @deprecated since 8.1: query part held/set directly on the content view search document model
     */
    @Deprecated
    public String getQueryPart() {
        DeprecationLogger.log("Query part held/set directly on the content view search document model", "8.1");
        return queryPart;
    }

    /**
     * @deprecated since 8.1: query part held/set directly on the content view search document model
     */
    public void setQueryPart(String queryPart) {
        DeprecationLogger.log("Query part held/set directly on the content view search document model", "8.1");
        this.queryPart = queryPart;
        addToQueryPartHistory(queryPart);
    }

    /**
     * @deprecated since 8.1: selected columns directly held/set on the content view search document model
     */
    @Deprecated
    public List<String> getSelectedLayoutColumns() {
        DeprecationLogger.log("Selected columns held/set directly on the content view search document model", "8.1");
        return selectedLayoutColumns;
    }

    /**
     * @deprecated since 8.1: selected columns directly held/set on the content view search document model
     */
    @Deprecated
    public void setSelectedLayoutColumns(List<String> selectedLayoutColumns) {
        DeprecationLogger.log("Selected columns held/set directly on the content view search document model", "8.1");
        this.selectedLayoutColumns = selectedLayoutColumns;
    }

    /**
     * @deprecated since 8.1: selected columns directly held/set on the content view search document model
     */
    @Deprecated
    public void resetSelectedLayoutColumns() {
        DeprecationLogger.log("Selected columns held/set directly on the content view search document model", "8.1");
        setSelectedLayoutColumns(null);
    }

    /**
     * @deprecated since 8.1: search sort infos directly held/set on the content view search document model
     */
    @Deprecated
    public List<SortInfo> getSearchSortInfos() {
        DeprecationLogger.log("Search sort infos held/set directly on the content view search document model", "8.1");
        return searchSortInfos;
    }

    /**
     * @deprecated since 8.1: search sort infos directly held/set on the content view search document model
     */
    @Deprecated
    public void setSearchSortInfos(List<SortInfo> searchSortInfos) {
        DeprecationLogger.log("Search sort infos held/set directly on the content view search document model", "8.1");
        this.searchSortInfos = searchSortInfos;
    }

    public void initCurrentSmartQuery(String existingQueryPart, boolean resetHistory) {
        currentSmartQuery = new IncrementalSmartNXQLQuery(existingQueryPart);
        if (resetHistory) {
            queryPartHistory = null;
            addToQueryPartHistory(existingQueryPart);
        }
    }

    /**
     * Creates a new {@link #currentSmartQuery} instance.
     * <p>
     * This method is supposed to be called once when loading a new page: it will initialize the smart query object
     * according to the current existing qury part.
     * <p>
     * It should not be called when there are validation errors happening on the page, otherwise the new query part may
     * be discarded.
     */
    public void initCurrentSmartQuery(String existingQueryPart) {
        initCurrentSmartQuery(existingQueryPart, true);
    }

    public SmartQuery getCurrentSmartQuery() {
        if (currentSmartQuery == null) {
            initCurrentSmartQuery("", true);
        }
        return currentSmartQuery;
    }

    /**
     * Updates the current {@link #currentSmartQuery} instance according to changes on the existing query part.
     */
    public void queryPartChanged(AjaxBehaviorEvent event) {
        UIComponent comp = event.getComponent();
        if (comp instanceof EditableValueHolder) {
            EditableValueHolder queryComp = (EditableValueHolder) comp;
            String newQuery = (String) queryComp.getSubmittedValue();
            // set local value in case of validation error in ajax region
            // when adding a new item to the query
            queryComp.setValue(newQuery);
            // update query
            currentSmartQuery.setExistingQueryPart(newQuery);
            addToQueryPartHistory(newQuery);
        } else {
            throw new NuxeoException("Component not found");
        }
    }

    /**
     * Updates the JSF component holding the query part.
     *
     * @param event the JSF event that will give an anchor on the JSF tree to find the target component.
     * @param newQuery the new query to set.
     * @param rebuildSmartQuery if true, will rebuild the smart query completely, otherwise will just set the query part
     *            on it.
     * @throws NuxeoException if target JSF component is not found in the JSF tree.
     */
    protected void setQueryPart(ActionEvent event, String newQuery, boolean rebuildSmartQuery) {
        if (currentSmartQuery != null) {
            UIComponent component = event.getComponent();
            if (component == null) {
                return;
            }
            // find component to update in the hierarchy of JSF components:
            // this is specific to rendering structure...
            EditableValueHolder queryPartComp = ComponentUtils.getComponent(component, queryPartComponentId,
                    EditableValueHolder.class);
            if (queryPartComp != null) {
                // set submitted value to ensure validation
                queryPartComp.setSubmittedValue(newQuery);
                // set local value in case of validation error in ajax region
                // when adding a new item to the query
                queryPartComp.setValue(newQuery);
                if (rebuildSmartQuery) {
                    // rebuild smart query
                    initCurrentSmartQuery(newQuery, false);
                } else {
                    currentSmartQuery.setExistingQueryPart(newQuery);
                }
                addToQueryPartHistory(newQuery);
            } else {
                throw new NuxeoException("Component not found");
            }
        }
    }

    /**
     * Updates the query part, asking the {@link #currentSmartQuery} to build the new resulting query.
     *
     * @see #setQueryPart(ActionEvent, String, boolean)
     */
    public void buildQueryPart(ActionEvent event) {
        if (currentSmartQuery != null) {
            String newQuery = currentSmartQuery.buildQuery();
            setQueryPart(event, newQuery, true);
        }
    }

    /**
     * Sets the query part to an empty value.
     *
     * @see #setQueryPart(ActionEvent, String, boolean)
     */
    public void clearQueryPart(ActionEvent event) {
        setQueryPart(event, "", false);
    }

    protected String getCurrentQueryPart() {
        if (currentSmartQuery != null) {
            return currentSmartQuery.getExistingQueryPart();
        }
        return null;
    }

    protected boolean hasQueryPartHistory(HistoryList<String> history) {
        if (history == null || history.isEmpty()) {
            return false;
        }
        String lastQueryPart = history.getLast();
        // lastQueryPart cannot be null
        if (history.size() == 1 && lastQueryPart.equals(getCurrentQueryPart())) {
            return false;
        }
        return true;
    }

    public boolean getCanUndoQueryPartChanges() {
        return hasQueryPartHistory(queryPartHistory);
    }

    public boolean getCanRedoQueryPartChanges() {
        return hasQueryPartHistory(redoQueryPartHistory);
    }

    public void undoHistoryChanges(ActionEvent event, HistoryList<String> history, HistoryList<String> redoHistory) {
        if (!hasQueryPartHistory(history)) {
            return;
        }
        String lastQueryPart = history.getLast();
        history.removeLast();
        String currentQueryPart = getCurrentQueryPart();
        // lastQueryPart cannot be null
        if (!lastQueryPart.equals(currentQueryPart)) {
            setQueryPart(event, lastQueryPart, false);
        } else if (history.size() > 0) {
            lastQueryPart = history.getLast();
            setQueryPart(event, lastQueryPart, false);
            history.removeLast();
        }
        if (redoHistory != null) {
            addToHistory(currentQueryPart, redoHistory);
        }
    }

    public void undoQueryPartChanges(ActionEvent event) {
        if (redoQueryPartHistory == null) {
            redoQueryPartHistory = new HistoryList<String>(HISTORY_CAPACITY);
        }
        undoHistoryChanges(event, queryPartHistory, redoQueryPartHistory);
    }

    public void redoQueryPartChanges(ActionEvent event) {
        undoHistoryChanges(event, redoQueryPartHistory, null);
    }

    protected void addToHistory(String queryPart, HistoryList<String> queryPartHistory) {
        if (queryPartHistory == null) {
            return;
        }
        if (queryPart == null) {
            queryPart = "";
        }
        if (queryPartHistory.size() == 0) {
            queryPartHistory.addLast(queryPart);
        } else {
            String lastQueryPart = queryPartHistory.getLast();
            if (!queryPart.equals(lastQueryPart)) {
                queryPartHistory.addLast(queryPart);
            }
        }
    }

    protected void addToQueryPartHistory(String queryPart) {
        if (queryPartHistory == null) {
            queryPartHistory = new HistoryList<String>(HISTORY_CAPACITY);
        }
        addToHistory(queryPart, queryPartHistory);
    }

    /**
     * Validates the query part: throws a {@link ValidatorException} if query is not a String, or if
     * {@link IncrementalSmartNXQLQuery#isValid(String)} returns false.
     *
     * @see IncrementalSmartNXQLQuery#isValid(String)
     */
    public void validateQueryPart(FacesContext context, UIComponent component, Object value) {
        if (value == null) {
            return;
        }
        if (!(value instanceof String)) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, "error.smart.query.invalidQuery"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
        String query = (String) value;
        if (StringUtils.isBlank(query)) {
            return;
        }
        if (!IncrementalSmartNXQLQuery.isValid(query)) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, "error.smart.query.invalidQuery"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

    /**
     * Returns true if current request is an ajax request.
     * <p>
     * Useful when some component should be required only when the global form is submitted, and not when ajax calls are
     * performed.
     */
    public boolean isAjaxRequest() {
        DeprecationLogger.log(
                "smartNXQLQueryActions#isAjaxRequest is not needed anymore, proper ajax calls make it possible to validate or not a field depending on use cases.",
                "8.1");
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            return context.getPartialViewContext().isAjaxRequest();
        }
        return false;
    }

    /**
     * Returns a valid where clause from a query part.
     * <p>
     * Useful to avoid generating an invalid query if query part is empty (especially if content view is not marked as
     * waiting for first execution).
     *
     * @since 8.1
     */
    public String getWhereClause(String queryPart, boolean followedByClause) {
        if (StringUtils.isBlank(queryPart)) {
            if (followedByClause) {
                return "WHERE ";
            }
            return "";
        }
        return "WHERE (" + queryPart + ")" + (followedByClause ? " AND " : "");
    }

    /**
     * @since 8.1
     */
    public boolean isInitialized() {
        return currentSmartQuery != null;
    }

    /**
     * @since 8.1
     */
    @Observer(value = { SearchUIActions.SEARCH_SELECTED_EVENT }, create = false)
    @BypassInterceptors
    public void resetCurrentSmartQuery() {
        currentSmartQuery = null;
    }

}
