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
package org.nuxeo.ecm.webapp.localconfiguration.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.webapp.localconfiguration.search.RepositoryInit.PATH_FOLDER;
import static org.nuxeo.ecm.webapp.localconfiguration.search.RepositoryInit.PATH_WORKSPACE;
import static org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfigurationConstants.FIELD_ADVANCED_SEARCH_VIEW;
import static org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfigurationConstants.SEARCH_LOCAL_CONFIGURATION_FACET;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 *
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", init = RepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.webapp.base" })
@LocalDeploy("org.nuxeo.ecm.webapp.base:test-search-local-configuration.xml")
public class TestSearchLocalConfiguration {

    private static final Log log = LogFactory.getLog(TestSearchLocalConfiguration.class);

    @Inject
    protected CoreSession session;

    @Test
    public void testDefautImportType() throws Exception {
        assertNotNull(session);
        DocumentModel workspace = session.getDocument(new PathRef(
                PATH_WORKSPACE));
        assertNotNull(workspace);

        workspace.setPropertyValue(FIELD_ADVANCED_SEARCH_VIEW, "fakeView");
        session.saveDocument(workspace);

        DocumentModel folder = session.getDocument(new PathRef(PATH_FOLDER));
        SearchLocalConfiguration configuration = getConfiguration(folder);
        assertNotNull(configuration);
        assertEquals("fakeView", configuration.getAdvancedSearchView());

    }

    protected static SearchLocalConfiguration getConfiguration(DocumentModel doc) {
        SearchLocalConfiguration configuration = null;
        try {
            LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
            configuration = localConfigurationService.getConfiguration(
                    SearchLocalConfiguration.class,
                    SEARCH_LOCAL_CONFIGURATION_FACET, doc);
        } catch (Exception e) {
            log.warn("failed to get configuration", e);
        }
        return configuration;
    }

}
