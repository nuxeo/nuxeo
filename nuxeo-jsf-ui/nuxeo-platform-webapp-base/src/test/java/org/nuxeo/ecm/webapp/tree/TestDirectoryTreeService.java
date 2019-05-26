/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.webapp.tree;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeDescriptor;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.actions:OSGI-INF/actions-framework.xml")
@Deploy("org.nuxeo.ecm.webapp.base:OSGI-INF/directorytreemanager-framework.xml")
@Deploy("org.nuxeo.ecm.webapp.base.tests:test-directory-tree-contrib.xml")
public class TestDirectoryTreeService {

    @Inject
    protected DirectoryTreeService directoryTreeService;

    @Inject
    protected ActionManager actionService;

    @Test
    public void shouldRegisterActionContribution() {
        assertNotNull(directoryTreeService);
        assertNotNull(actionService);
        assertTrue(directoryTreeService.getDirectoryTrees().contains("anyNavigation"));
        Action action = actionService.getAction(DirectoryTreeDescriptor.ACTION_ID_PREFIX + "anyNavigation");
        assertNotNull(action);
    }
}
