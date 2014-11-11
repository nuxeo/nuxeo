/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     alexandre
 */
package org.nuxeo.ecm.platform.jbpm.web;

import java.util.Arrays;

import junit.framework.TestCase;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;

/**
 * @author alexandre
 */
public class JbpmHelperTest extends TestCase {

    private final JbpmHelper helper = new JbpmHelper();

    public void testIsAssignedToUser() {
        TaskInstance ti = new TaskInstance();
        UserPrincipal principal = new UserPrincipal("linnet", Arrays.asList(
                "g1", "g2"), false, false);
        ti.setActorId(NuxeoPrincipal.PREFIX + "linnet");
        assertTrue(helper.isTaskAssignedToUser(ti, principal));
        ti.setActorId(NuxeoPrincipal.PREFIX + "joe");
        assertFalse(helper.isTaskAssignedToUser(ti, principal));
        ti.setActorId(null);
        ti.setPooledActors(new String[] { NuxeoPrincipal.PREFIX + "bob",
                NuxeoGroup.PREFIX + "trudy" });
        assertFalse(helper.isTaskAssignedToUser(ti, principal));
        ti.setPooledActors(new String[] { NuxeoPrincipal.PREFIX + "bob",
                NuxeoGroup.PREFIX + "g1" });
        assertTrue(helper.isTaskAssignedToUser(ti, principal));
    }

}
