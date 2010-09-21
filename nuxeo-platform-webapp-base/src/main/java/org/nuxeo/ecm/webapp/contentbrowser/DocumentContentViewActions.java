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
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentView;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles available content views defined on a document type per category
 *
 * @author Anahide Tchertchian
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

    // API for content view support

    protected List<ContentViewHeader> getContentViewHeaders(TypeInfo typeInfo,
            String category) throws ClientException {
        List<ContentViewHeader> res = new ArrayList<ContentViewHeader>();
        String[] cvNames = typeInfo.getContentViews(category);
        if (cvNames != null) {
            for (String cvName : cvNames) {
                ContentView cv = contentViewActions.getContentView(cvName);
                if (cv != null) {
                    res.add(new ContentViewHeader(cvName, cv.getTitle(),
                            cv.getTranslateTitle(), cv.getIconPath()));
                }
            }
        }
        return res;
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
        String docType = doc.getType();
        if (!typeToContentView.containsKey(docType)) {
            Map<String, List<ContentViewHeader>> byCategories = new HashMap<String, List<ContentViewHeader>>();
            typeToContentView.put(docType, byCategories);
        }
        if (!typeToContentView.get(docType).containsKey(category)) {
            Map<String, List<ContentViewHeader>> byCategories = typeToContentView.get(docType);
            TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
            byCategories.put(category,
                    getContentViewHeaders(typeInfo, category));
            typeToContentView.put(docType, byCategories);
        }
        return !typeToContentView.get(docType).get(category).isEmpty();
    }

    public List<ContentViewHeader> getAvailableContentViewsForDocument(
            DocumentModel doc, String category) throws ClientException {
        if (doc == null || !hasContentViewSupport(doc, category)) {
            return Collections.emptyList();
        }
        // call to hasContentViewSupport should have filled the type cache
        return typeToContentView.get(doc.getType()).get(category);
    }

    public List<ContentViewHeader> getAvailableContentViewsForCurrentDocument(
            String category) throws ClientException {
        if (currentAvailableContentViews == null
                || !currentAvailableContentViews.containsKey(category)) {
            if (currentAvailableContentViews == null) {
                currentAvailableContentViews = new HashMap<String, List<ContentViewHeader>>();
            }
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentAvailableContentViews.put(category,
                    getAvailableContentViewsForDocument(currentDocument,
                            category));
        }
        return currentAvailableContentViews.get(category);
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChanged() {
        currentAvailableContentViews = null;
    }

}
