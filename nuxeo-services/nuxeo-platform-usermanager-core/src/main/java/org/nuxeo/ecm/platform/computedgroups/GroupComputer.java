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
* Interface that must be implemented by all contributed {@link GroupComputer}
*
* @author Thierry Delprat
*
*/
public interface GroupComputer {


    /**
     *
     * Return the group names for a give User
     *
     * @param nuxeoPrincipal
     * @return
     * @throws Exception
     */
    List<String> getGroupsForUser(NuxeoPrincipalImpl nuxeoPrincipal) throws Exception ;

    /**
     * return all group ids.
     * If you class can not efficiently compute this list, you can return an empty list.
     * In this case you need to implement the searchGroups method
     *
     * @return
     * @throws Exception
     */
    List<String> getAllGroupIds() throws Exception ;

    /**
     *
     * Return the members for a give group
     *
     * @param groupName
     * @return
     * @throws Exception
     */
    List<String> getGroupMembers(String groupName) throws Exception;

    /**
     * return parent groups
     *
     * @param groupName
     * @return
     * @throws Exception
     */
    List<String> getParentsGroupNames(String groupName) throws Exception;

    /**
     * Return children groups
     *
     * @param groupName
     * @return
     * @throws Exception
     */
    List<String> getSubGroupsNames(String groupName) throws Exception;

    /**
     * Search for a group.
     * (This method is used in particular from UI to search/select a group)
     *
     * @param filter
     * @param fulltext
     * @return
     * @throws Exception
     */
    List<String> searchGroups(Map<String, Serializable> filter,HashSet<String> fulltext) throws Exception;

}
