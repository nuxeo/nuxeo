/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     eugen
 */
package org.nuxeo.ecm.platform.localconfiguration.search;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfigurationConstants.SEARCH_LOCAL_CONFIGURATION_FACET;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfiguration;
import org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfigurationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 *
 */

@Name("searchConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class SearchConfigurationActions implements Serializable {

    private static final Log log = LogFactory
            .getLog(SearchConfigurationActions.class);

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public String getAdvancedSearchView() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        SearchLocalConfiguration configuration = null;
        try {
            LocalConfigurationService localConfigurationService = Framework
                    .getService(LocalConfigurationService.class);
            configuration = localConfigurationService.getConfiguration(
                    SearchLocalConfiguration.class,
                    SEARCH_LOCAL_CONFIGURATION_FACET, currentDoc);
            if (isLocalConfigurationExistsAndSearchViewAvailable(configuration)) {
                return configuration.getAdvancedSearchView();
            }
        } catch (Exception e) {
            log.debug(
                    "failed to get search configuration for "
                            + currentDoc.getPathAsString(), e);
        }
        return SearchLocalConfigurationConstants.DEFAULT_ADVANCED_SEARCH_VIEW;

    }

    protected boolean isLocalConfigurationExistsAndSearchViewAvailable(
            SearchLocalConfiguration configuration) {
        return configuration != null
                && configuration.getAdvancedSearchView() != null;
    }
}
