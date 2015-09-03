/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
