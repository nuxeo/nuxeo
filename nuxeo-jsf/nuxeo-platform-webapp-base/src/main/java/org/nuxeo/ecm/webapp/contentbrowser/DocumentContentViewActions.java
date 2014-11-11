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
package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles available content views defined on a document type per category
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@Name("documentContentViewActions")
@Scope(CONVERSATION)
public class DocumentContentViewActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    /**
     * Map caching content views defined on a given document type
     */
    protected Map<String, Map<String, List<ContentViewHeader>>> typeToContentView = new HashMap<String, Map<String, List<ContentViewHeader>>>();

    protected Map<String, List<ContentViewHeader>> currentAvailableContentViews;

    /**
     * Map caching content views shown in export defined on a given document
     * type
     */
    protected Map<String, Map<String, List<ContentViewHeader>>> typeToExportContentView = new HashMap<String, Map<String, List<ContentViewHeader>>>();

    protected Map<String, List<ContentViewHeader>> currentExportContentViews;

    // API for content view support

    protected Map<String, List<ContentViewHeader>> getContentViewHeaders(
            TypeInfo typeInfo, boolean export) throws ClientException {
        Map<String, List<ContentViewHeader>> res = new LinkedHashMap<String, List<ContentViewHeader>>();
        Map<String, String[]> cvNamesByCat;
        if (export) {
            cvNamesByCat = typeInfo.getContentViewsForExport();
        } else {
            cvNamesByCat = typeInfo.getContentViews();
        }
        if (cvNamesByCat != null) {
            for (Map.Entry<String, String[]> cvNameByCat : cvNamesByCat.entrySet()) {
                List<ContentViewHeader> headers = new ArrayList<ContentViewHeader>();
                String[] cvNames = cvNameByCat.getValue();
                if (cvNames != null) {
                    for (String cvName : cvNames) {
                        ContentView cv = contentViewActions.getContentView(cvName);
                        if (cv != null) {
                            headers.add(new ContentViewHeader(cvName,
                                    cv.getTitle(), cv.getTranslateTitle(),
                                    cv.getIconPath()));
                        }
                    }
                }
                res.put(cvNameByCat.getKey(), headers);
            }
        }
        return res;
    }

    protected void retrieveContentViewHeaders(DocumentModel doc)
            throws ClientException {
        String docType = doc.getType();
        if (!typeToContentView.containsKey(docType)) {
            TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
            Map<String, List<ContentViewHeader>> byCategories = getContentViewHeaders(
                    typeInfo, false);
            typeToContentView.put(docType, byCategories);
        }
    }

    protected void retrieveExportContentViewHeaders(DocumentModel doc)
            throws ClientException {
        String docType = doc.getType();
        if (!typeToExportContentView.containsKey(docType)) {
            TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
            Map<String, List<ContentViewHeader>> byCategories = getContentViewHeaders(
                    typeInfo, true);
            typeToExportContentView.put(docType, byCategories);
        }
    }

    /**
     * Returns true if content views are defined on given document for given
     * category.
     * <p>
     * Also fetches content view headers defined on a document type
     */
    public boolean hasContentViewSupport(DocumentModel doc, String category)
            throws ClientException {
        if (doc == null) {
            return false;
        }
        retrieveContentViewHeaders(doc);
        String docType = doc.getType();
        if (!typeToContentView.get(docType).containsKey(category)) {
            return false;
        }
        return !typeToContentView.get(docType).get(category).isEmpty();
    }

    public Map<String, List<ContentViewHeader>> getAvailableContentViewsForDocument(
            DocumentModel doc) throws ClientException {
        if (doc == null) {
            return Collections.emptyMap();
        }
        retrieveContentViewHeaders(doc);
        String docType = doc.getType();
        return typeToContentView.get(docType);
    }

    public List<ContentViewHeader> getAvailableContentViewsForDocument(
            DocumentModel doc, String category) throws ClientException {
        if (doc == null) {
            return Collections.emptyList();
        }
        retrieveContentViewHeaders(doc);
        String docType = doc.getType();
        if (!typeToContentView.get(docType).containsKey(category)) {
            return Collections.emptyList();
        }
        return typeToContentView.get(doc.getType()).get(category);
    }

    public Map<String, List<ContentViewHeader>> getAvailableContentViewsForCurrentDocument()
            throws ClientException {
        if (currentAvailableContentViews == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentAvailableContentViews = getAvailableContentViewsForDocument(currentDocument);
        }
        return currentAvailableContentViews;
    }

    public List<ContentViewHeader> getAvailableContentViewsForCurrentDocument(
            String category) throws ClientException {
        getAvailableContentViewsForCurrentDocument();
        return currentAvailableContentViews.get(category);
    }

    public Map<String, List<ContentViewHeader>> getExportContentViewsForDocument(
            DocumentModel doc) throws ClientException {
        if (doc == null) {
            return Collections.emptyMap();
        }
        retrieveExportContentViewHeaders(doc);
        String docType = doc.getType();
        return typeToExportContentView.get(docType);
    }

    public Map<String, List<ContentViewHeader>> getExportContentViewsForCurrentDocument()
            throws ClientException {
        if (currentExportContentViews == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentExportContentViews = getExportContentViewsForDocument(currentDocument);
        }
        return currentExportContentViews;
    }

    public List<ContentViewHeader> getExportContentViewsForCurrentDocument(
            String category) throws ClientException {
        getExportContentViewsForCurrentDocument();
        return currentExportContentViews.get(category);
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChanged() {
        currentAvailableContentViews = null;
        currentExportContentViews = null;
    }

}
