/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager;

import java.util.List;

import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestFileManagerComponent extends NXRuntimeTestCase {

    private FileManagerService filemanagerService;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle(FileManagerUTConstants.MIMETYPE_BUNDLE);
        deployContrib(FileManagerUTConstants.FILEMANAGER_BUNDLE,
                "OSGI-INF/nxfilemanager-service.xml");

        deployContrib(FileManagerUTConstants.FILEMANAGER_TEST_BUNDLE,
                "nxfilemanager-test-contribs.xml");

        filemanagerService = (FileManagerService) Framework.getRuntime().getComponent(
                FileManagerService.NAME);
    }

    @Override
    public void tearDown() throws Exception {
        filemanagerService = null;

        undeployContrib(FileManagerUTConstants.FILEMANAGER_TEST_BUNDLE,
                "nxfilemanager-test-contribs.xml");

        undeployContrib(FileManagerUTConstants.FILEMANAGER_BUNDLE,
                "OSGI-INF/nxfilemanager-service.xml");

        super.tearDown();
    }

    public void testPlugins() {
        FileImporter testPlu = filemanagerService.getPluginByName("plug");
        List<String> filters = testPlu.getFilters();
        assertEquals(2, filters.size());
    }

}
