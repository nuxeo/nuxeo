/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.theme.test.localconfiguration;


import org.junit.runner.RunWith;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.theme.localconfiguration.LocalThemeConfig;
import org.nuxeo.theme.localconfiguration.LocalThemeConfigConstants;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = LocalConfigurationRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.theme.styling",
        "org.nuxeo.theme.styling.tests:local-configuration-config.xml" })
public class TestLocalConfiguration {

    public static final DocumentRef PARENT_WORKSPACE_REF = new PathRef(
            "/default-domain/workspaces/workspace");

    public static final DocumentRef CHILD_WORKSPACE_REF = new PathRef(
            "/default-domain/workspaces/workspace/workspace2");

    public static final String WORKSPACE_TYPE = "Workspace";

    @Inject
    protected CoreSession session;

    @Inject
    protected LocalConfigurationService localConfigurationService;

    @Test
    public void testLocalTheme() throws Exception {
        DocumentModel workspace = session.getDocument(PARENT_WORKSPACE_REF);
        workspace.setPropertyValue(
                LocalThemeConfigConstants.THEME_CONFIGURATION_THEME_PROPERTY,
                "galaxy");
        workspace.setPropertyValue(
                LocalThemeConfigConstants.THEME_CONFIGURATION_PAGE_PROPERTY,
                "default");
        workspace.setPropertyValue(
                LocalThemeConfigConstants.THEME_CONFIGURATION_FLAVOR_PROPERTY,
                "dark");
        session.saveDocument(workspace);
        session.save();

        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        LocalThemeConfig configuration = localConfigurationService.getConfiguration(
                LocalThemeConfig.class,
                LocalThemeConfigConstants.THEME_CONFIGURATION_FACET, workspace);
        assertNotNull(configuration);
        assertEquals("galaxy/default", configuration.computePagePath());
        assertEquals("dark", configuration.getFlavor());
    }

}
