/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.search.ui.seam;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.search.ui.localconfiguration.Constants.SEARCH_CONFIGURATION_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewHeader;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.action.ActionContextProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.search.ui.SearchUIService;
import org.nuxeo.search.ui.localconfiguration.SearchConfiguration;

@Name("searchUIConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class SearchUIConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    @In(create = true)
    protected transient ContentViewService contentViewService;

    public List<ContentViewHeader> getSelectedContentViewHeaders() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return getSelectedContentViewHeaders(currentDoc);
    }

    public List<ContentViewHeader> getSelectedContentViewHeaders(DocumentModel document) {
        if (!document.hasFacet(SEARCH_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        SearchUIService searchUIService = Framework.getService(SearchUIService.class);
        List<ContentViewHeader> contentViewHeaders = searchUIService.getContentViewHeaders(actionContextProvider.createActionContext());

        List<String> allowedContentViewNames = getAllowedContentViewNames(document);
        if (allowedContentViewNames.isEmpty()) {
            LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
            SearchConfiguration configuration = localConfigurationService.getConfiguration(SearchConfiguration.class,
                    SEARCH_CONFIGURATION_FACET, document);
            if (configuration == null) {
                return contentViewHeaders;
            }
            allowedContentViewNames = configuration.getAllowedContentViewNames();
        }

        if (allowedContentViewNames.isEmpty()) {
            return contentViewHeaders;
        }

        List<ContentViewHeader> selectedContentViewHeaders = new ArrayList<>();
        for (ContentViewHeader contentViewHeader : contentViewHeaders) {
            if (allowedContentViewNames.contains(contentViewHeader.getName())) {
                selectedContentViewHeaders.add(contentViewHeader);
            }
        }

        return selectedContentViewHeaders;
    }

    public List<ContentViewHeader> getNotSelectedContentViewHeaders() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return getNotSelectedContentViewHeaders(currentDoc);
    }

    public List<ContentViewHeader> getNotSelectedContentViewHeaders(DocumentModel document) {
        if (!document.hasFacet(SEARCH_CONFIGURATION_FACET)) {
            return Collections.emptyList();
        }

        List<ContentViewHeader> notSelectedContentViewHeaders = new ArrayList<>();
        List<ContentViewHeader> selectedContentViewHeaders = getSelectedContentViewHeaders(document);
        SearchUIService searchUIService = Framework.getService(SearchUIService.class);
        List<ContentViewHeader> contentViewHeaders = searchUIService.getContentViewHeaders(actionContextProvider.createActionContext());
        for (ContentViewHeader contentViewHeader : contentViewHeaders) {
            if (!selectedContentViewHeaders.contains(contentViewHeader)) {
                notSelectedContentViewHeaders.add(contentViewHeader);
            }
        }

        return notSelectedContentViewHeaders;
    }

    protected List<String> getAllowedContentViewNames(DocumentModel doc) {
        SearchConfiguration adapter = doc.getAdapter(SearchConfiguration.class);
        if (adapter == null) {
            return Collections.emptyList();
        }

        return adapter.getAllowedContentViewNames();
    }
}
