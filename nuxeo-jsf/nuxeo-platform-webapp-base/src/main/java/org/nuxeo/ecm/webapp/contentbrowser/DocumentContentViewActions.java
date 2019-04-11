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
package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.platform.types.localconfiguration.ContentViewConfigurationConstants.CONTENT_VIEW_CONFIGURATION_CATEGORY;
import static org.nuxeo.ecm.platform.types.localconfiguration.ContentViewConfigurationConstants.CONTENT_VIEW_CONFIGURATION_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.types.localconfiguration.ContentViewConfiguration;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.action.ActionContextProvider;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles available content views defined on a document type per category, as well as helper methods to retrieve
 * selection actions for a given content view.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@Name("documentContentViewActions")
@Scope(CONVERSATION)
public class DocumentContentViewActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentContentViewActions.class);

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    @In(create = true)
    protected ContentViewService contentViewService;

    /**
     * Map caching content views defined on a given document type
     */
    protected Map<String, Map<String, List<ContentViewHeader>>> typeToContentView = new HashMap<>();

    protected Map<String, List<ContentViewHeader>> currentAvailableContentViews;

    /**
     * Map caching content views shown in export defined on a given document type
     */
    protected Map<String, Map<String, List<ContentViewHeader>>> typeToExportContentView = new HashMap<>();

    protected Map<String, List<ContentViewHeader>> currentExportContentViews;

    // API for content view support

    protected Map<String, List<ContentViewHeader>> getContentViewHeaders(TypeInfo typeInfo, boolean export)
            {
        Map<String, List<ContentViewHeader>> res = new LinkedHashMap<>();
        Map<String, String[]> cvNamesByCat;
        if (export) {
            cvNamesByCat = typeInfo.getContentViewsForExport();
        } else {
            cvNamesByCat = typeInfo.getContentViews();
        }
        if (cvNamesByCat != null) {
            for (Map.Entry<String, String[]> cvNameByCat : cvNamesByCat.entrySet()) {
                List<ContentViewHeader> headers = new ArrayList<>();
                String[] cvNames = cvNameByCat.getValue();
                if (cvNames != null) {
                    for (String cvName : cvNames) {
                        ContentViewHeader header = contentViewService.getContentViewHeader(cvName);
                        if (header != null) {
                            headers.add(header);
                        }
                    }
                }
                res.put(cvNameByCat.getKey(), headers);
            }
        }
        return res;
    }

    protected void retrieveContentViewHeaders(DocumentModel doc) {
        String docType = doc.getType();
        if (!typeToContentView.containsKey(docType)) {
            TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
            Map<String, List<ContentViewHeader>> byCategories = getContentViewHeaders(typeInfo, false);
            typeToContentView.put(docType, byCategories);
        }
    }

    protected void retrieveExportContentViewHeaders(DocumentModel doc) {
        String docType = doc.getType();
        if (!typeToExportContentView.containsKey(docType)) {
            TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
            Map<String, List<ContentViewHeader>> byCategories = getContentViewHeaders(typeInfo, true);
            typeToExportContentView.put(docType, byCategories);
        }
    }

    /**
     * Returns true if content views are defined on given document for given category.
     * <p>
     * Also fetches content view headers defined on a document type
     */
    public boolean hasContentViewSupport(DocumentModel doc, String category) {
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

    public Map<String, List<ContentViewHeader>> getAvailableContentViewsForDocument(DocumentModel doc)
            {
        if (doc == null) {
            return Collections.emptyMap();
        }
        retrieveContentViewHeaders(doc);
        String docType = doc.getType();
        return typeToContentView.get(docType);
    }

    public List<ContentViewHeader> getAvailableContentViewsForDocument(DocumentModel doc, String category)
            {
        if (doc == null) {
            return Collections.emptyList();
        }
        if (CONTENT_VIEW_CONFIGURATION_CATEGORY.equals(category)) {
            List<ContentViewHeader> localHeaders = getLocalConfiguredContentViews(doc);
            if (localHeaders != null) {
                return localHeaders;
            }
        }
        retrieveContentViewHeaders(doc);
        String docType = doc.getType();
        if (!typeToContentView.get(docType).containsKey(category)) {
            return Collections.emptyList();
        }
        return typeToContentView.get(doc.getType()).get(category);
    }

    public Map<String, List<ContentViewHeader>> getAvailableContentViewsForCurrentDocument() {
        if (currentAvailableContentViews == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentAvailableContentViews = getAvailableContentViewsForDocument(currentDocument);
        }
        return currentAvailableContentViews;
    }

    public List<ContentViewHeader> getAvailableContentViewsForCurrentDocument(String category) {
        if (CONTENT_VIEW_CONFIGURATION_CATEGORY.equals(category)) {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            List<ContentViewHeader> localHeaders = getLocalConfiguredContentViews(currentDoc);
            if (localHeaders != null) {
                return localHeaders;
            }
        }
        getAvailableContentViewsForCurrentDocument();
        return currentAvailableContentViews.get(category);
    }

    public List<ContentViewHeader> getLocalConfiguredContentViews(DocumentModel doc) {
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        ContentViewConfiguration configuration = localConfigurationService.getConfiguration(
                ContentViewConfiguration.class, CONTENT_VIEW_CONFIGURATION_FACET, doc);
        if (configuration == null) {
            return null;
        }
        List<String> cvNames = configuration.getContentViewsForType(doc.getType());
        if (cvNames == null) {
            return null;
        }
        List<ContentViewHeader> headers = new ArrayList<>();
        for (String cvName : cvNames) {
            ContentViewHeader header = contentViewService.getContentViewHeader(cvName);
            if (header != null) {
                headers.add(header);
            }
        }
        if (!headers.isEmpty()) {
            return headers;
        }
        return null;
    }

    public Map<String, List<ContentViewHeader>> getExportContentViewsForDocument(DocumentModel doc)
            {
        if (doc == null) {
            return Collections.emptyMap();
        }
        retrieveExportContentViewHeaders(doc);
        String docType = doc.getType();
        return typeToExportContentView.get(docType);
    }

    public Map<String, List<ContentViewHeader>> getExportContentViewsForCurrentDocument() {
        if (currentExportContentViews == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentExportContentViews = getExportContentViewsForDocument(currentDocument);
        }
        return currentExportContentViews;
    }

    public List<ContentViewHeader> getExportContentViewsForCurrentDocument(String category) {
        getExportContentViewsForCurrentDocument();
        return currentExportContentViews.get(category);
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChanged() {
        currentAvailableContentViews = null;
        currentExportContentViews = null;
    }

    /**
     * Resets typeToContentView cache on {@link EventNames#FLUSH_EVENT}, triggered by hot reload when dev mode is set.
     *
     * @since 5.7.1
     */
    @Observer(value = { EventNames.FLUSH_EVENT })
    @BypassInterceptors
    public void onHotReloadFlush() {
        typeToContentView = new HashMap<>();
    }

    /**
     * Helper to retrieve selection actions for a given content view, passing additional variables to the
     * {@link ActionContext} used for resolution.
     *
     * @since 2.17
     * @param category
     * @param contentView
     * @param selectedEntries
     */
    public List<Action> getSelectionActions(String category, ContentView contentView, List<Object> selectedEntries) {
        ActionContext ctx = actionContextProvider.createActionContext();
        ctx.putLocalVariable("contentView", contentView);
        ctx.putLocalVariable("selectedDocuments", selectedEntries);
        return webActions.getActionsList(category, ctx, false);
    }
}
