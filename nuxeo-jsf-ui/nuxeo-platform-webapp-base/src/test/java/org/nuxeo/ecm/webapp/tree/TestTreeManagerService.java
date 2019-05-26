/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
package org.nuxeo.ecm.webapp.tree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.webapp.base:OSGI-INF/nxtreemanager-framework.xml")
@Deploy("org.nuxeo.ecm.webapp.base:OSGI-INF/nxtreemanager-contrib.xml")
public class TestTreeManagerService {

    @Inject
    public TreeManager treeManager;

    @Test
    public void testDefaultContribs() {
        String filterName = "navigation";
        assertEquals("tree_children", treeManager.getPageProviderName(filterName));
        assertNull(treeManager.getFilter(filterName));
        assertNotNull(treeManager.getLeafFilter(filterName));
        assertNull(treeManager.getSorter(filterName));
    }

    @Test
    @Deploy("org.nuxeo.ecm.webapp.base.tests:test-nxtreemanager-contrib.xml")
    public void testOverride() throws Exception {
        String filterName = "navigation";
        assertEquals("tree_children", treeManager.getPageProviderName(filterName));
        assertNotNull(treeManager.getFilter(filterName));
        assertNull(treeManager.getLeafFilter(filterName));
        assertNotNull(treeManager.getSorter(filterName));
    }

}
