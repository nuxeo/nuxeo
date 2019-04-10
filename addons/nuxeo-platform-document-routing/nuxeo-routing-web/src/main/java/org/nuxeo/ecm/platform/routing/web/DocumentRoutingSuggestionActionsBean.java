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
 *    Mariana Cedica
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.routing.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.MAX_RESULTS_PROPERTY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.PAGE_SIZE_RESULTS_KEY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.runtime.api.Framework;

/**
 * Retrieves relations for current document route
 *
 * @author Mariana Cedica
 */
@Name("docRoutingSuggestionActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class DocumentRoutingSuggestionActionsBean extends DocumentContextBoundActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CURRENT_DOC_ROUTING_SEARCH_ATTACHED_DOC = "CURRENT_DOC_ROUTING_SEARCH_ATTACHED_DOC";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public DocumentModel getDocumentModel(String id) {
        return documentManager.getDocument(new IdRef(id));
    }

    public List<DocumentModel> getDocumentSuggestions(Object input) {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(MAX_RESULTS_PROPERTY, PAGE_SIZE_RESULTS_KEY);
        props.put(CORE_SESSION_PROPERTY, (Serializable) documentManager);
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                CURRENT_DOC_ROUTING_SEARCH_ATTACHED_DOC, null, null, 0L, props, String.format("%s%%", input));
        return pageProvider.getCurrentPage();
    }

    public List<DocumentModel> getRouteModelSuggestions(Object input) {
        DocumentRoutingService documentRoutingService = Framework.getService(DocumentRoutingService.class);
        return documentRoutingService.searchRouteModels(documentManager, (String) input);
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
    }

}
