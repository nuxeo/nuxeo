/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 * *
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * Interface for computed group service.
 *
 * @author tiry
 */
public interface ComputedGroupsService {

    boolean activateComputedGroups();

    /**
     * Returns list of ids for users computed groups.
     */
    List<String> computeGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal);

    /**
     * Resolves a computed group by it's name.
     */
    NuxeoGroup getComputedGroup(String groupName);

    /**
     * Defines if a computed group can override a physical group.
     */
    boolean allowGroupOverride();

    /**
     * Returns list of all computed groups (if this is available).
     */
    List<String> computeGroupIds();

    /**
     * Retrieves member users for a given computed group.
     */
    List<String> getComputedGroupMembers(String groupName);

    /**
     * Retrieves parent group for a given computed group.
     */
    List<String> getComputedGroupParent(String groupName);

    /**
     * Retrieves sub groups for a given computed group.
     */
    List<String> getComputedGroupSubGroups(String groupName);

    /**
     * Searches for a computed group.
     */
    List<String> searchComputedGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext);

}
