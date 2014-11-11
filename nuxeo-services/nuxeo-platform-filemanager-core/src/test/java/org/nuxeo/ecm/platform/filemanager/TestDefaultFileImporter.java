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
package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.filemanager.RepositoryInit.PATH_FOLDER;
import static org.nuxeo.ecm.platform.filemanager.RepositoryInit.PATH_WORKSPACE;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DEFAULT_TYPE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.service.extension.DefaultFileImporter;
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
@Deploy({ "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.core" })
@LocalDeploy("org.nuxeo.ecm.platform.filemanager.core:test-ui-types-local-configuration.xml")
public class TestDefaultFileImporter {

    @Inject
    protected CoreSession session;

    @Test
    public void testDefautImportType() throws Exception {
        assertNotNull(session);
        DocumentModel workspace = session.getDocument(new PathRef(
                PATH_WORKSPACE));
        assertNotNull(workspace);
        DocumentModel folder = session.getDocument(new PathRef(PATH_FOLDER));
        assertNotNull(folder);

        assertEquals("File", DefaultFileImporter.getTypeName(folder));

        workspace.setPropertyValue(UI_TYPES_CONFIGURATION_DEFAULT_TYPE,
                "FakeFile");
        session.saveDocument(workspace);
        session.save();

        assertEquals("FakeFile", DefaultFileImporter.getTypeName(folder));

    }

}
