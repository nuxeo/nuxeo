/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ataillefer
 */
package org.nuxeo.ecm.platform.task.core;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;

/**
 * Test TaskActorsHelper.
 *
 * @author ataillefer
 */
public class TaskActorsHelperTest {

    /**
     * Test get task actors.
     */
    @Test
    public void testGetTaskActors() {

        final String username = "joe";

        // Test unprefixed user name
        NuxeoPrincipal principal = new UserPrincipal(username, null, false, false);

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
        List<String> groups = new ArrayList<>();
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
