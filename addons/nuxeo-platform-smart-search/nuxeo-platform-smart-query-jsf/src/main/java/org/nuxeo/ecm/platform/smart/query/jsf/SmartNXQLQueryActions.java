/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.query.jsf;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.smart.query.HistoryList;
import org.nuxeo.ecm.platform.smart.query.SmartQuery;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * Seam component handling a {@link IncrementalSmartNXQLQuery} instance created
 * for a given existing query string.
 * <p>
 * Also handles undo/redo actions and all ajax interactions for an incremental
 * query build page.
 *
 * @since 5.4
 * @author Anahide Tchertchian
 */
@Name("smartNXQLQueryActions")
@Scope(ScopeType.CONVERSATION)
public class SmartNXQLQueryActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int HISTORY_CAPACITY = 20;

    protected String queryPart;

    protected HistoryList<String> queryPartHistory;

    protected HistoryList<String> redoQueryPartHistory;

    protected IncrementalSmartNXQLQuery currentSmartQuery;

    protected List<String> selectedLayoutColumns;

    protected List<SortInfo> searchSortInfos;

    /**
     * Request parameter passed from the widget that makes it possible to
     * decide whether the backing object should be updated when performing ajax
     * calls.
     * <p>
     * For instance, on the smart search form, any ajax action should update
     * the backing property {@link #queryPart}. When the query part is held by
     * a document property, it should not be updated on ajax actions: only the
     * global submit of the form should impact it.
     */
    @RequestParameter
    protected Boolean updateQueryPart;

    /**
     * Request parameter making it possible to find the component holding the
     * query part to update it.
     */
    @RequestParameter
    protected String queryPartComponentId;

    public String getQueryPart() {
        return queryPart;
    }

    public void setQueryPart(String queryPart) {
        this.queryPart = queryPart;
        addToQueryPartHistory(queryPart);
    }

    public List<String> getSelectedLayoutColumns() {
        return selectedLayoutColumns;
    }

    public void setSelectedLayoutColumns(List<String> selectedLayoutColumns) {
        this.selectedLayoutColumns = selectedLayoutColumns;
    }

    public void resetSelectedLayoutColumns() {
        setSelectedLayoutColumns(null);
    }

    public List<SortInfo> getSearchSortInfos() {
        return searchSortInfos;
    }

    public void setSearchSortInfos(List<SortInfo> searchSortInfos) {
        this.searchSortInfos = searchSortInfos;
    }

    public void initCurrentSmartQuery(String existingQueryPart,
            boolean resetHistory) {
        currentSmartQuery = new IncrementalSmartNXQLQuery(existingQueryPart);
        if (resetHistory) {
            queryPartHistory = null;
            addToQueryPartHistory(existingQueryPart);
        }
    }

    /**
     * Creates a new {@link #currentSmartQuery} instance.
     * <p>
     * This method is supposed to be called once when loading a new page: it
     * will initialize the smart query object according to the current existing
     * qury part.
     * <p>
     * It should not be called when there are validation errors happening on
     * the page, otherwise the new query part may be discarded.
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
     * Updates the current {@link #currentSmartQuery} instance according to
     * changes on the existing query part.
     * <p>
     * if request parameters {@link #updateQueryPart} is true, the field
     * {@link #queryPart} will also be edited.
     */
    public void queryPartChanged(ActionEvent event) throws ClientException {
        UIComponent comp = event.getComponent();
        UIComponent parent = comp.getParent();
        if (parent instanceof EditableValueHolder) {
            EditableValueHolder queryComp = (EditableValueHolder) parent;
            String newQuery = (String) queryComp.getSubmittedValue();
            // set local value in case of validation error in ajax region
            // when adding a new item to the query
            queryComp.setValue(newQuery);
            // update query
            currentSmartQuery.setExistingQueryPart(newQuery);
            if (Boolean.TRUE.equals(updateQueryPart)) {
                // also set it on source component in case user navigates
                // somewhere else
                setQueryPart(newQuery);
            } else {
                addToQueryPartHistory(newQuery);
            }
        } else {
            throw new ClientException("Component not found");
        }
    }

    /**
     * Updates the JSF component holding the query part.
     * <p>
     * if request parameters {@link #updateQueryPart} is true, the field
     * {@link #queryPart} will also be edited.
     *
     * @param event the JSF event that will give an anchor on the JSF tree to
     *            find the target component.
     * @param newQuery the new query to set.
     * @param rebuildSmartQuery if true, will rebuild the smart query
     *            completely, otherwise will just set the query part on it.
     * @throws ClientException if target JSF component is not found in the JSF
     *             tree.
     */
    protected void setQueryPart(ActionEvent event, String newQuery,
            boolean rebuildSmartQuery) throws ClientException {
        if (currentSmartQuery != null) {
            UIComponent component = event.getComponent();
            if (component == null) {
                return;
            }
            // find component to update in the hierarchy of JSF components:
            // this is specific to rendering structure...
            EditableValueHolder queryPartComp = ComponentUtils.getComponent(
                    component, queryPartComponentId, EditableValueHolder.class);
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
                if (Boolean.TRUE.equals(updateQueryPart)) {
                    // also set current query part in case user navigates
                    // somewhere else
                    setQueryPart(newQuery);
                } else {
                    addToQueryPartHistory(newQuery);
                }
            } else {
                throw new ClientException("Component not found");
            }
        }
    }

    /**
     * Updates the query part, asking the {@link #currentSmartQuery} to build
     * the new resulting query.
     *
     * @see #setQueryPart(ActionEvent, String)
     */
    public void buildQueryPart(ActionEvent event) throws ClientException {
        if (currentSmartQuery != null) {
            String newQuery = currentSmartQuery.buildQuery();
            setQueryPart(event, newQuery, true);
        }
    }

    /**
     * Sets the query part to an empty value.
     *
     * @see #setQueryPart(ActionEvent, String)
     */
    public void clearQueryPart(ActionEvent event) throws ClientException {
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

    public void undoHistoryChanges(ActionEvent event,
            HistoryList<String> history, HistoryList<String> redoHistory)
            throws ClientException {
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

    public void undoQueryPartChanges(ActionEvent event) throws ClientException {
        if (redoQueryPartHistory == null) {
            redoQueryPartHistory = new HistoryList<String>(HISTORY_CAPACITY);
        }
        undoHistoryChanges(event, queryPartHistory, redoQueryPartHistory);
    }

    public void redoQueryPartChanges(ActionEvent event) throws ClientException {
        undoHistoryChanges(event, redoQueryPartHistory, null);
    }

    protected void addToHistory(String queryPart,
            HistoryList<String> queryPartHistory) {
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
     * Validates the query part: throws a {@link ValidatorException} if query
     * is not a String, or if {@link IncrementalSmartNXQLQuery#isValid(String)}
     * returns false.
     *
     * @see IncrementalSmartNXQLQuery#isValid(String)
     */
    public void validateQueryPart(FacesContext context, UIComponent component,
            Object value) {
        if (value == null || !(value instanceof String)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "error.smart.query.invalidQuery"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
        String query = (String) value;
        if (!IncrementalSmartNXQLQuery.isValid(query)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "error.smart.query.invalidQuery"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

    /**
     * Returns true if current request is an ajax request.
     * <p>
     * Useful when some component should be required only when the global form
     * is submitted, and not when ajax calls are performed.
     */
    public boolean isAjaxRequest() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            ExternalContext eContext = context.getExternalContext();
            Map<String, String> requestMap = eContext.getRequestParameterMap();
            if (requestMap != null) {
                String ajax = requestMap.get("AJAXREQUEST");
                if (ajax != null) {
                    return true;
                }
            }
        }
        return false;
    }

}
