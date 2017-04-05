/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.List;

/**
 * Holds the list of member users and subgroups for a group.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface NuxeoGroup extends Serializable {

    String PREFIX = "group:";

    /**
     * Gets the list of member users of this group.
     *
     * @return the list of member users of this group
     */
    List<String> getMemberUsers();

    /**
     * Gets the list of member groups of this group.
     *
     * @return the list of member groups of this group
     */
    List<String> getMemberGroups();

    /**
     * Gets the list of groups this group is a member of.
     *
     * @return the list of parent groups of this group
     */
    List<String> getParentGroups();

    /**
     * Sets the list of member users for this group.
     *
     * @param users a list of users
     */
    void setMemberUsers(List<String> users);

    /**
     * Sets the list of member groups for this group.
     *
     * @param groups a list of groups
     */
    void setMemberGroups(List<String> groups);

    /**
     * Sets the list of groups this group is member of.
     *
     * @param groups a list of groups
     */
    void setParentGroups(List<String> groups);

    /**
     * Gets the name of the group.
     *
     * @return the name of the group
     */
    String getName();

    /**
     * Sets the name of this group.
     *
     * @param name the new name of the group
     */
    void setName(String name);

    /**
     * Gets the label of the group
     *
     * @return the label of the group
     */
    String getLabel();

    /**
     * Sets the label of this group.
     *
     * @param label the label of the group
     */
    void setLabel(String label);

}
