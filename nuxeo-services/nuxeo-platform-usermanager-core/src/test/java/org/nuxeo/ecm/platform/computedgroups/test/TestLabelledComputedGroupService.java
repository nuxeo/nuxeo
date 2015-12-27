/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.computedgroups.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.computedgroups.ComputedGroupsService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class) // to init properties for SQL datasources
@Deploy({ "org.nuxeo.ecm.core.schema", //
        "org.nuxeo.ecm.core.api", //
        "org.nuxeo.ecm.core", //
        "org.nuxeo.ecm.core.event", //
        "org.nuxeo.ecm.platform.usermanager.api", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.directory.api", //
        "org.nuxeo.ecm.directory.types.contrib", //
        "org.nuxeo.ecm.directory", //
        "org.nuxeo.ecm.directory.sql", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.usermanager.tests:labelled-computedgroups-framework.xml", //
        "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml", //
})
public class TestLabelledComputedGroupService {

    @Inject
    protected ComputedGroupsService cgs;

    @Test
    public void testContrib() throws Exception {

        NuxeoGroup group = cgs.getComputedGroup("Grp1");
        assertNotNull(group);
        assertEquals("Groupe 1", group.getLabel());

        group = cgs.getComputedGroup("Grp2");
        assertNotNull(group);
        assertEquals("Groupe 2", group.getLabel());

    }

}
