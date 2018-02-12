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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.directory.localconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FACET;
import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FIELD;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.4.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DirectoryLocalConfigurationRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory:types-for-test-directory-local-configuration.xml")
public class TestDirectoryLocalConfigurationDefinition {

    public static final DocumentRef PARENT_DOMAIN_REF = new PathRef("/default-domain");

    public static final DocumentRef CHILD_WORKSPACE_REF = new PathRef("/default-domain/workspaces/workspace/workspace2");

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
    public void shouldReturnANullSuffixValueIfLocalConfigurationNotSet() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_DOMAIN_REF);
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);

        DirectoryConfiguration configuration = localConfigurationService.getConfiguration(DirectoryConfiguration.class,
                DIRECTORY_CONFIGURATION_FACET, workspace);

        assertNull(configuration.getDirectorySuffix());
    }

    @Test
    public void shouldReturnSuffixGivenByLocalConfig() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_DOMAIN_REF);

        setDirectorySuffix(workspace, "suffix");

        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        DirectoryConfiguration configuration = localConfigurationService.getConfiguration(DirectoryConfiguration.class,
                DIRECTORY_CONFIGURATION_FACET, workspace);

        assertNotNull(configuration);
        assertEquals("suffix", configuration.getDirectorySuffix());
    }

    @Test
    public void shouldReturnSuffixGivenByLocalConfigWithTrim() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_DOMAIN_REF);

        setDirectorySuffix(workspace, "  suffix     ");

        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        DirectoryConfiguration configuration = localConfigurationService.getConfiguration(DirectoryConfiguration.class,
                DIRECTORY_CONFIGURATION_FACET, workspace);

        assertNotNull(configuration);
        assertEquals("suffix", configuration.getDirectorySuffix());
    }

    protected void setDirectorySuffix(DocumentModel doc, String directorySuffix) {
        doc.setPropertyValue(DIRECTORY_CONFIGURATION_FIELD, directorySuffix);
        session.saveDocument(doc);
        session.save();
    }

}
