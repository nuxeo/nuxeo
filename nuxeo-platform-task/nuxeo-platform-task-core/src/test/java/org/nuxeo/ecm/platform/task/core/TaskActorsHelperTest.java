/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.platform.task.core;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;

/**
 * Test TaskActorsHelper.
 * 
 * @author ataillefer
 */
public class TaskActorsHelperTest extends TestCase {

    /**
     * Test get task actors.
     */
    @Test
    public void testGetTaskActors() {

        final String username = "joe";

        // Test unprefixed user name
        NuxeoPrincipal principal = new UserPrincipal(username, null, false,
                false);

        List<String> actors = TaskActorsHelper.getTaskActors(principal);
        assertEquals(2, actors.size());
        assertTrue(actors.contains("joe"));
        assertTrue(actors.contains("user:joe"));

        // Test prefixed user name
        principal.setName("user:joe");

        actors = TaskActorsHelper.getTaskActors(principal);
        assertEquals(2, actors.size());
        assertTrue(actors.contains("joe"));
        assertTrue(actors.contains("user:joe"));

        // Test unprefixed group names
        List<String> groups = new ArrayList<String>();
        groups.add("marketing");
        groups.add("sales");
        principal.setGroups(groups);

        actors = TaskActorsHelper.getTaskActors(principal);
        assertEquals(6, actors.size());
        assertTrue(actors.contains("joe"));
        assertTrue(actors.contains("user:joe"));
        assertTrue(actors.contains("marketing"));
        assertTrue(actors.contains("group:marketing"));
        assertTrue(actors.contains("sales"));
        assertTrue(actors.contains("group:sales"));

        // Test prefixed group names
        groups.clear();
        groups.add("group:marketing");
        groups.add("group:sales");
        principal.setGroups(groups);

        actors = TaskActorsHelper.getTaskActors(principal);
        assertEquals(6, actors.size());
        assertTrue(actors.contains("joe"));
        assertTrue(actors.contains("user:joe"));
        assertTrue(actors.contains("marketing"));
        assertTrue(actors.contains("group:marketing"));
        assertTrue(actors.contains("sales"));
        assertTrue(actors.contains("group:sales"));

    }
}
