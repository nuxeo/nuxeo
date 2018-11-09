/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 * *
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
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
     * Update the virtual groups of the user with the computed groups.
     */
    void updateGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal);

    /**
     * Resolves a computed group by it's name.
     * @deprecated since 9.3
     */
    @Deprecated
    NuxeoGroup getComputedGroup(String groupName);

    /**
     * Resolves a computed group by it's name.
     * @since 9.3
     */
    NuxeoGroup getComputedGroup(String groupName, GroupConfig groupConfig);

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
    List<String> searchComputedGroups(Map<String, Serializable> filter, Set<String> fulltext);

    /**
     * Searches for computed groups.
     *
     * @param queryBuilder the query
     * @return the list of computed group ids
     * @since 10.3
     */
    List<String> searchComputedGroups(QueryBuilder queryBuilder);

}
