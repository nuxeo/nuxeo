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
package org.nuxeo.ecm.platform.task.core.helpers;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Helper class for dealing with task actors.
 * <p>
 * Provides a method to build the task actors list.
 *
 * @author ataillefer
 */
public final class TaskActorsHelper {

    /**
     * Prevent instantiation.
     */
    private TaskActorsHelper() {
        // Final class
    }

    /**
     * Gets the task actors list: prefixed and unprefixed names of the principal and all its groups.
     *
     * @param principal the principal
     * @return the actors and group
     */
    public static List<String> getTaskActors(NuxeoPrincipal principal) {

        List<String> actors = new ArrayList<>();

        String name = principal.getName();
        actors.add(name);
        if (!name.startsWith(NuxeoPrincipal.PREFIX)) {
            actors.add(NuxeoPrincipal.PREFIX + name);
        } else {
            actors.add(name.substring(NuxeoPrincipal.PREFIX.length()));
        }
        for (String group : principal.getAllGroups()) {
            actors.add(group);
            if (!group.startsWith(NuxeoGroup.PREFIX)) {
                actors.add(NuxeoGroup.PREFIX + group);
            } else {
                actors.add(group.substring(NuxeoGroup.PREFIX.length()));
            }
        }

        return actors;
    }
}
