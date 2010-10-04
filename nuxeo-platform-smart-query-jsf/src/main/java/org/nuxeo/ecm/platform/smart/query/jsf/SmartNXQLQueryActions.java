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

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.platform.smart.query.SmartQuery;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author Anahide Tchertchian
 */
@Name("smartNXQLQueryActions")
@Scope(ScopeType.CONVERSATION)
public class SmartNXQLQueryActions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String queryPart;

    protected SmartQuery currentSmartQuery;

    protected List<String> selectedLayoutColumns;

    protected List<SortInfo> searchSortInfos;

    @RequestParameter
    protected String baseComponentId;

    @RequestParameter
    protected String queryStringComponentId;

    public String getQueryPart() {
        return queryPart;
    }

    public void setQueryPart(String queryPart) {
        this.queryPart = queryPart;
    }

    public Literal getQueryPartAsLiteral() {
        if (queryPart == null) {
            return new StringLiteral("");
        } else {
            return new StringLiteral(queryPart);
        }
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

    public SortInfo getNewSortInfo() {
        return new SortInfo("", true);
    }

    public void initCurrentSmartQuery(String existingQueryPart) {
        currentSmartQuery = new NXQLIncrementalSmartQuery(existingQueryPart);
    }

    public void queryPartChanged(ActionEvent event) throws ClientException {
        UIComponent comp = event.getComponent();
        UIComponent parent = comp.getParent();
        if (parent instanceof EditableValueHolder) {
            String newValue = (String) ((EditableValueHolder) parent).getSubmittedValue();
            // rebuild smart query
            currentSmartQuery = new NXQLIncrementalSmartQuery(newValue);
        } else {
            throw new ClientException("Component not found");
        }
    }

    public SmartQuery getCurrentSmartQuery() {
        if (currentSmartQuery == null) {
            currentSmartQuery = new NXQLIncrementalSmartQuery("");
        }
        return currentSmartQuery;
    }

    public void buildQueryString(ActionEvent event) throws ClientException {
        if (currentSmartQuery != null) {
            UIComponent component = event.getComponent();
            if (component == null) {
                return;
            }
            // find component to update in the hierarchy of JSF components:
            // this is specific to rendering structure...
            UIComponent commonAncestor = component.getParent().getParent();
            UIComponent base = ComponentUtils.getComponent(commonAncestor,
                    baseComponentId, UIComponent.class);
            EditableValueHolder queryStringComp = ComponentUtils.getComponent(
                    base, queryStringComponentId, EditableValueHolder.class);
            if (queryStringComp != null) {
                queryStringComp.setSubmittedValue(currentSmartQuery.buildQuery());
            } else {
                throw new ClientException("Component not found");
            }
        }
    }

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
        if (!NXQLIncrementalSmartQuery.isValid(query)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "error.smart.query.invalidQuery"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

}
