/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 *
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
     * @return the list of member groups of this group
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
