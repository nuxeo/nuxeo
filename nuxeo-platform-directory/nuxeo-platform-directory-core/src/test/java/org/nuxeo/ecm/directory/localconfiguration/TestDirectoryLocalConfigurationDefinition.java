/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.directory.localconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FACET;
import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.4.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DirectoryLocalConfigurationRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.api" })
@LocalDeploy("org.nuxeo.ecm.directory:types-for-test-directory-local-configuration.xml")
public class TestDirectoryLocalConfigurationDefinition {

    public static final DocumentRef PARENT_DOMAIN_REF = new PathRef(
            "/default-domain");

    public static final DocumentRef CHILD_WORKSPACE_REF = new PathRef(
            "/default-domain/workspaces/workspace/workspace2");

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected LocalConfigurationService localConfigurationService;
    
    @Before
    public void disableListeners() throws Exception {
        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        eventAdmin.setBulkModeEnabled(true);
        eventAdmin.setListenerEnabledFlag("sql-storage-binary-text", false);
    }

    @Test
    public void shouldReturnANullSuffixValueIfLocalConfigurationNotSet()
            throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_DOMAIN_REF);
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);

        DirectoryConfiguration configuration = localConfigurationService.getConfiguration(
                DirectoryConfiguration.class, DIRECTORY_CONFIGURATION_FACET,
                workspace);

        assertNull(configuration.getDirectorySuffix());
    }

    @Test
    public void shouldReturnSuffixGivenByLocalConfig()
            throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_DOMAIN_REF);

        setDirectorySuffix(workspace, "suffix");

        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        DirectoryConfiguration configuration = localConfigurationService.getConfiguration(
                DirectoryConfiguration.class, DIRECTORY_CONFIGURATION_FACET,
                workspace);

        assertNotNull(configuration);
        assertEquals("suffix", configuration.getDirectorySuffix());
    }

    @Test
    public void shouldReturnSuffixGivenByLocalConfigWithTrim()
            throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_DOMAIN_REF);

        setDirectorySuffix(workspace, "  suffix     ");

        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        DirectoryConfiguration configuration = localConfigurationService.getConfiguration(
                DirectoryConfiguration.class, DIRECTORY_CONFIGURATION_FACET,
                workspace);

        assertNotNull(configuration);
        assertEquals("suffix", configuration.getDirectorySuffix());
    }

    protected void setDirectorySuffix(DocumentModel doc, String directorySuffix)
            throws ClientException {
        doc.setPropertyValue(DIRECTORY_CONFIGURATION_FIELD, directorySuffix);
        session.saveDocument(doc);
        session.save();
    }

}
