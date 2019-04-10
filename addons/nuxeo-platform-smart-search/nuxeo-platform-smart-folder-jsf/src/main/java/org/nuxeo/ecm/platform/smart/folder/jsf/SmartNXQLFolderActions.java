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
package org.nuxeo.ecm.platform.smart.folder.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.smart.query.jsf.SmartNXQLQueryActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;

/**
 * Provides methods to save a global smart search in a document.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
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
     * Initializes a document model of given type, and fill its properties
     * according to fields set on the seam component
     * {@link SmartNXQLFolderActions}.
     * <p>
     * Assumes the document type holds the 'content_view_display' and
     * 'smart_folder' schemas.
     */
    public String saveQueryAsDocument(String docType) throws ClientException {
        documentActions.createDocument(docType);
        // fill in information from smart search
        DocumentModel doc = navigationContext.getChangeableDocument();
        String queryPart = smartNXQLQueryActions.getQueryPart();
        List<String> selectedLayoutColumns = smartNXQLQueryActions.getSelectedLayoutColumns();
        List<SortInfo> sortInfos = smartNXQLQueryActions.getSearchSortInfos();
        doc.setPropertyValue(SmartFolderDocumentConstants.QUERY_PART_PROP_NAME,
                queryPart);
        doc.setPropertyValue(
                SmartFolderDocumentConstants.SELECTED_LAYOUT_COLUMNS_PROP_NAME,
                (Serializable) selectedLayoutColumns);
        List<Map<String, Serializable>> sortInfosForDoc = new ArrayList<Map<String, Serializable>>();
        if (sortInfos != null) {
            for (SortInfo sortInfo : sortInfos) {
                if (sortInfo != null) {
                    sortInfosForDoc.add(SortInfo.asMap(sortInfo));
                }
            }
        }
        doc.setPropertyValue(SmartFolderDocumentConstants.SORT_INFOS_PROP_NAME,
                (Serializable) sortInfosForDoc);
        return "save_smart_query_as_document";
    }
}
