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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
@Deploy("org.nuxeo.ecm.platform.filemanager.core:OSGI-INF/nxfilemanager-service.xml")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.tests:nxfilemanager-test-contribs.xml")
public class TestFileManagerComponent {

    @Test
    public void testPlugins() {
        FileManagerService filemanagerService = (FileManagerService) Framework.getRuntime()
                                                                              .getComponent(FileManagerService.NAME);
        FileImporter testPlu = filemanagerService.getPluginByName("plug");
        List<String> filters = testPlu.getFilters();
        assertEquals(2, filters.size());
    }

}
