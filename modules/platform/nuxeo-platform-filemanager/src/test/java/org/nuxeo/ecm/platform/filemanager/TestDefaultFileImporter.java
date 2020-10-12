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
 *     eugen
 */
package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.filemanager.RepositoryInit.PATH_FOLDER;
import static org.nuxeo.ecm.platform.filemanager.RepositoryInit.PATH_WORKSPACE;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DEFAULT_TYPE;

import javax.inject.Inject;

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

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.filemanager:test-ui-types-local-configuration.xml")
public class TestDefaultFileImporter {

    @Inject
    protected CoreSession session;

    @Test
    public void testDefautImportType() throws Exception {
        assertNotNull(session);
        DocumentModel workspace = session.getDocument(new PathRef(PATH_WORKSPACE));
        assertNotNull(workspace);
        DocumentModel folder = session.getDocument(new PathRef(PATH_FOLDER));
        assertNotNull(folder);

        assertEquals("File", DefaultFileImporter.getTypeName(folder));

        workspace.setPropertyValue(UI_TYPES_CONFIGURATION_DEFAULT_TYPE, "FakeFile");
        session.saveDocument(workspace);
        session.save();

        assertEquals("FakeFile", DefaultFileImporter.getTypeName(folder));

    }

}
