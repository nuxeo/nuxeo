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
     * Gets the task actors list: prefixed and unprefixed names of the principal
     * and all its groups.
     * 
     * @param principal the principal
     * @return the actors and group
     */
    public static List<String> getTaskActors(NuxeoPrincipal principal) {

        List<String> actors = new ArrayList<String>();

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
