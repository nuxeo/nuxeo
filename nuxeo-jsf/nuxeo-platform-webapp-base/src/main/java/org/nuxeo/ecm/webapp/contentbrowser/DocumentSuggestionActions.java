/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides suggestion methods on documents
 *
 * @since 5.4.2
 */
@Name("docSuggestionActions")
@Scope(ScopeType.PAGE)
public class DocumentSuggestionActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_PAGE_PROVIDER_NAME = "default_document_suggestion";

    @In(required = true)
    protected transient CoreSession documentManager;

    @RequestParameter
    protected String pageProviderName;

    /**
     * If true, '*' will be appended to the pattern to do a prefix search.
     *
     * @since 5.7.2
     */
    @RequestParameter
    protected Boolean prefixSearch;

    protected String cachedPageProviderName;

    protected Object cachedInput;

    protected List<DocumentModel> cachedSuggestions;

    public List<DocumentModel> getDocSuggestions(Object input) {
        if (equals(cachedPageProviderName, pageProviderName) && equals(cachedInput, input)) {
            return cachedSuggestions;
        }

        String pattern = (String) input;
        PageProvider<DocumentModel> pp = getPageProvider(pattern);
        List<DocumentModel> documents = pp.getCurrentPage();
        cachedInput = input;
        cachedSuggestions = documents;
        return documents;
    }

    /**
     * Returns suggestion documents matching given pattern.
     * <p>
     * Search is performed using the page provider referenced in the widget definition (defaults to
     * {@link #DEFAULT_PAGE_PROVIDER_NAME}).
     * <p>
     * Since 5.7, if the page provider defines parameters, they're added to the implicit parameter (the pattern) that
     * stays the first one to be referenced in the search query.
     *
     * @param pattern: the input as typed by user in suggestion box
     */
    @SuppressWarnings("unchecked")
    protected PageProvider<DocumentModel> getPageProvider(String pattern) {
        String ppName = DEFAULT_PAGE_PROVIDER_NAME;
        if (pageProviderName != null && !StringUtils.isEmpty(pageProviderName)) {
            ppName = pageProviderName;
        }
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) documentManager);
        // resolve additional parameters
        PageProviderDefinition ppDef = ppService.getPageProviderDefinition(ppName);
        String[] params = ppDef.getQueryParameters();
        if (params == null) {
            params = new String[0];
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Object[] resolvedParams = new Object[params.length + 1];

        if (Boolean.TRUE.equals(prefixSearch) && !pattern.endsWith(" ")) {
            pattern += "*";
        }

        resolvedParams[0] = pattern;
        for (int i = 0; i < params.length; i++) {
            resolvedParams[i + 1] = ComponentTagUtils.resolveElExpression(context, params[i]);
        }
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(ppName, null, null,
                null, props, resolvedParams);
        if (pp == null) {
            throw new NuxeoException("Page provider not found: " + ppName);
        }
        return pp;
    }

    public boolean getDocumentExistsAndIsVisibleWithId(String id) {
        if (StringUtils.isEmpty(id)) {
            return false;
        }
        IdRef ref = new IdRef(id);
        return documentManager.exists(ref) && documentManager.hasPermission(ref, SecurityConstants.READ);
    }

    public boolean getDocumentExistsAndIsVisibleWithPath(String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        PathRef ref = new PathRef(path);
        return documentManager.exists(ref) && documentManager.hasPermission(ref, SecurityConstants.READ);
    }

    public DocumentModel getDocumentWithId(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return documentManager.getDocument(new IdRef(id));
    }

    public DocumentModel getDocumentWithPath(String path) {
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        return documentManager.getDocument(new PathRef(path));
    }

    protected boolean equals(Object item1, Object item2) {
        if (item1 == null && item2 == null) {
            return true;
        } else if (item1 == null) {
            return false;
        } else {
            return item1.equals(item2);
        }
    }

}
