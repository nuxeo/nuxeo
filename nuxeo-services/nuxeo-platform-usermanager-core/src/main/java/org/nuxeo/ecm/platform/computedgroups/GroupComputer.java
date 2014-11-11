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
    List<String> getGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal)
            throws Exception;

    /**
     * Return all group ids. If you class can not efficiently compute this list,
     * you can return an empty list. In this case you need to implement the
     * searchGroups method.
     */
    List<String> getAllGroupIds() throws Exception;

    /**
     * Returns the members for a give group.
     */
    List<String> getGroupMembers(String groupName) throws Exception;

    /**
     * Return parent groups.
     */
    List<String> getParentsGroupNames(String groupName) throws Exception;

    /**
     * Returns children groups.
     */
    List<String> getSubGroupsNames(String groupName) throws Exception;

    /**
     * Searches for a group. (This method is used in particular from UI to
     * search/select a group).
     */
    List<String> searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws Exception;

    /**
     * Returns true if the given group exists.
     */
    boolean hasGroup(String name) throws Exception;

}
