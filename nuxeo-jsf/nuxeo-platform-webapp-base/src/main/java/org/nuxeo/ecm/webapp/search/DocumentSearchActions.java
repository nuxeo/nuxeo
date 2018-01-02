/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webapp.search;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles search parameters needed for simple and advanced and administrator searches.
 * <p>
 * Search parameters are referenced in the content views used on search form and result pages.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 * @deprecated since 6.0. Not used anymore, see NXP-14992.
 */
@Deprecated
@Name("documentSearchActions")
@Scope(ScopeType.CONVERSATION)
public class DocumentSearchActions implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Query parameter string used to perform full text search on all documents
     */
    protected String simpleSearchKeywords = "";

    protected String nxqlQuery = "";

    protected List<String> selectedLayoutColumns;

    protected List<SortInfo> searchSortInfos;

    public String getSimpleSearchKeywords() {
        return simpleSearchKeywords;
    }

    public void setSimpleSearchKeywords(String simpleSearchKeywords) {
        this.simpleSearchKeywords = simpleSearchKeywords;
    }

    public void validateSimpleSearchKeywords(FacesContext context, UIComponent component, Object value) {
        if (!(value instanceof String) || StringUtils.isEmpty(((String) value).trim())) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(context,
                    "feedback.search.noKeywords"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
        String[] keywords = ((String) value).trim().split(" ");
        for (String keyword : keywords) {
            if (keyword.startsWith("*")) {
                // Can't begin search with * character
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(context,
                        "feedback.search.star"), null);
                // also add global message
                context.addMessage(null, message);
                throw new ValidatorException(message);
            }
        }
    }

    public String getNxqlQuery() {
        return nxqlQuery;
    }

    public void setNxqlQuery(String nxqlQuery) {
        this.nxqlQuery = nxqlQuery;
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

    public Map<String, Serializable> getNewSortInfoMap() {
        SortInfo sortInfo = getNewSortInfo();
        return SortInfo.asMap(sortInfo);
    }

    @BypassInterceptors
    public void resetSearches() {
        simpleSearchKeywords = "";
        nxqlQuery = "";
    }

    /**
     * Resets cached selected columns/sort infos on hot reload when dev mode is set.
     *
     * @since 5.9.4
     */
    @Observer(value = { EventNames.FLUSH_EVENT }, create = false)
    @BypassInterceptors
    public void onHotReloadFlush() {
        selectedLayoutColumns = null;
        searchSortInfos = null;
    }

}
