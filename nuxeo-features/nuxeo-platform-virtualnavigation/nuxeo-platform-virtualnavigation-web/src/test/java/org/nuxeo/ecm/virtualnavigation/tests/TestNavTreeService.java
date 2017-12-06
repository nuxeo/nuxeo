/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.virtualnavigation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.webapp.tree.nav.NavTreeDescriptor;
import org.nuxeo.ecm.webapp.tree.nav.NavTreeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@LocalDeploy({ "org.nuxeo.ecm.platform.actions.core:OSGI-INF/actions-framework.xml",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/navtree-framework.xml",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/directorytreemanager-framework.xml",
        "org.nuxeo.ecm.webapp.base:OSGI-INF/navtree-default-contrib.xml",
        "org.nuxeo.platform.virtualnavigation.web:OSGI-INF/directorytreemanager-contrib.xml" })
public class TestNavTreeService {

    @Test
    public void testNavTreesWithDirectories() throws Exception {

        NavTreeService service = Framework.getService(NavTreeService.class);
        assertNotNull(service);

        List<NavTreeDescriptor> descs = service.getTreeDescriptors();
        assertEquals(3, descs.size());

        assertNotNull(descs.get(0).getXhtmlview());
        assertFalse(descs.get(0).isDirectoryTreeBased());

        assertNull(descs.get(1).getXhtmlview());
        assertTrue(descs.get(1).isDirectoryTreeBased());

        assertNull(descs.get(2).getXhtmlview());
        assertTrue(descs.get(2).isDirectoryTreeBased());

    }

}
