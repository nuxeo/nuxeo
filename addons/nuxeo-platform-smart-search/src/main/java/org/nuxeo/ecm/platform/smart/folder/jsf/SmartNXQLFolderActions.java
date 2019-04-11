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
package org.nuxeo.ecm.platform.smart.folder.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.smart.query.jsf.SmartNXQLQueryActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.runtime.logging.DeprecationLogger;

/**
 * Provides methods to save a global smart search in a document.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 * @deprecated since 8.1: use generic methods on SearchUIActionsBean
 */
@Deprecated
@Name("smartNXQLFolderActions")
@Scope(ScopeType.CONVERSATION)
public class SmartNXQLFolderActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = true)
    protected DocumentActions documentActions;

    @In(create = true, required = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = true)
    protected transient SmartNXQLQueryActions smartNXQLQueryActions;

    /**
     * Initializes a document model of given type, and fill its properties according to fields set on the seam component
     * {@link SmartNXQLFolderActions}.
     * <p>
     * Assumes the document type holds the 'content_view_display' and 'smart_folder' schemas.
     */
    public String saveQueryAsDocument(String docType) {
        DeprecationLogger.log(
                "Seam component names 'smartNXQLFolderActions' is deprecated, use component named 'searchUIActions' instead.",
                "8.1");
        documentActions.createDocument(docType);
        // fill in information from smart search
        DocumentModel doc = navigationContext.getChangeableDocument();
        String queryPart = smartNXQLQueryActions.getQueryPart();
        List<String> selectedLayoutColumns = smartNXQLQueryActions.getSelectedLayoutColumns();
        List<SortInfo> sortInfos = smartNXQLQueryActions.getSearchSortInfos();
        doc.setPropertyValue(SmartFolderDocumentConstants.QUERY_PART_PROP_NAME, queryPart);
        doc.setPropertyValue(SmartFolderDocumentConstants.SELECTED_LAYOUT_COLUMNS_PROP_NAME,
                (Serializable) selectedLayoutColumns);
        List<Map<String, Serializable>> sortInfosForDoc = new ArrayList<>();
        if (sortInfos != null) {
            for (SortInfo sortInfo : sortInfos) {
                if (sortInfo != null) {
                    sortInfosForDoc.add(SortInfo.asMap(sortInfo));
                }
            }
        }
        doc.setPropertyValue(SmartFolderDocumentConstants.SORT_INFOS_PROP_NAME, (Serializable) sortInfosForDoc);
        return "save_smart_query_as_document";
    }
}
