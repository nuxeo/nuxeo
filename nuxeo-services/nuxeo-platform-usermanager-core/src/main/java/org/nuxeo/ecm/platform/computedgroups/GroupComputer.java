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

import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * Interface that must be implemented by all contributed {@link GroupComputer}s.
 *
 * @author Thierry Delprat
 */
public interface GroupComputer {

    /**
     * Returns the group names for a give User.
     */
    List<String> getGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal);

    /**
     * Return all group ids. If you class can not efficiently compute this list, you can return an empty list. In this
     * case you need to implement the searchGroups method.
     */
    List<String> getAllGroupIds();

    /**
     * Returns the members for a give group.
     */
    List<String> getGroupMembers(String groupName);

    /**
     * Return parent groups.
     */
    List<String> getParentsGroupNames(String groupName);

    /**
     * Returns children groups.
     */
    List<String> getSubGroupsNames(String groupName);

    /**
     * Searches for a group. (This method is used in particular from UI to search/select a group).
     */
    List<String> searchGroups(Map<String, Serializable> filter, Set<String> fulltext);

    /**
     * Searches for groups.
     *
     * @param queryBuilder the query
     * @return the list of computed group ids
     * @since 10.3
     */
    List<String> searchGroups(QueryBuilder queryBuilder);

    /**
     * Returns true if the given group exists.
     */
    boolean hasGroup(String name);

}
