/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager;

import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestFileManagerComponent extends NXRuntimeTestCase {

    private FileManagerService filemanagerService;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle(FileManagerUTConstants.MIMETYPE_BUNDLE);
        deployContrib(FileManagerUTConstants.FILEMANAGER_BUNDLE, "OSGI-INF/nxfilemanager-service.xml");

        deployContrib(FileManagerUTConstants.FILEMANAGER_TEST_BUNDLE, "nxfilemanager-test-contribs.xml");

        filemanagerService = (FileManagerService) Framework.getRuntime().getComponent(FileManagerService.NAME);
    }

    @After
    public void tearDown() throws Exception {
        filemanagerService = null;

        undeployContrib(FileManagerUTConstants.FILEMANAGER_TEST_BUNDLE, "nxfilemanager-test-contribs.xml");

        undeployContrib(FileManagerUTConstants.FILEMANAGER_BUNDLE, "OSGI-INF/nxfilemanager-service.xml");

        super.tearDown();
    }

    @Test
    public void testPlugins() {
        FileImporter testPlu = filemanagerService.getPluginByName("plug");
        List<String> filters = testPlu.getFilters();
        assertEquals(2, filters.size());
    }

}
