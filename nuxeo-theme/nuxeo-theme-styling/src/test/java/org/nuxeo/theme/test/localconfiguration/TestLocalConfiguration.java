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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.theme.test.localconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.theme.localconfiguration.LocalThemeConfig;
import org.nuxeo.theme.localconfiguration.LocalThemeConfigConstants;

/**
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = LocalConfigurationRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.theme.styling", "org.nuxeo.theme.styling.tests:local-configuration-config.xml" })
public class TestLocalConfiguration {

    public static final DocumentRef PARENT_WORKSPACE_REF = new PathRef("/default-domain/workspaces/workspace");

    public static final DocumentRef CHILD_WORKSPACE_REF = new PathRef("/default-domain/workspaces/workspace/workspace2");

    public static final String WORKSPACE_TYPE = "Workspace";

    @Inject
    protected CoreSession session;

    @Inject
    protected LocalConfigurationService localConfigurationService;

    @Test
    public void testLocalTheme() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        workspace.setPropertyValue(LocalThemeConfigConstants.THEME_CONFIGURATION_THEME_PROPERTY, "galaxy");
        workspace.setPropertyValue(LocalThemeConfigConstants.THEME_CONFIGURATION_PAGE_PROPERTY, "default");
        workspace.setPropertyValue(LocalThemeConfigConstants.THEME_CONFIGURATION_FLAVOR_PROPERTY, "dark");
        session.saveDocument(workspace);
        session.save();

        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        LocalThemeConfig configuration = localConfigurationService.getConfiguration(LocalThemeConfig.class,
                LocalThemeConfigConstants.THEME_CONFIGURATION_FACET, workspace);
        assertNotNull(configuration);
        assertEquals("dark", configuration.getFlavor());
    }

}
