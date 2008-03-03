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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoGroup;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class NuxeoGroupImpl implements NuxeoGroup {

    private static final long serialVersionUID = -69828664399387083L;

    private final List<String> users;

    private final List<String> groups;

    private final List<String> parentGroups;

    private String name;

    public NuxeoGroupImpl(String name) {
        if (name == null) {
            throw new IllegalArgumentException("group name cannot be null");
        }
        this.name = name;
        users = new ArrayList<String>();
        groups = new ArrayList<String>();
        parentGroups = new ArrayList<String>();
    }

    public List<String> getMemberUsers() {
        return users;
    }

    public List<String> getMemberGroups() {
        return groups;
    }

    public List<String> getParentGroups() {
        return parentGroups;
    }

    public void setMemberUsers(List<String> users) {
        if (users == null) {
            throw new IllegalArgumentException("member users list cannot be null");
        }
        this.users.clear();
        this.users.addAll(users);
    }

    public void setMemberGroups(List<String> groups) {
        if (groups == null) {
            throw new IllegalArgumentException("member groups list cannot be null");
        }
        this.groups.clear();
        this.groups.addAll(groups);
    }

    public void setParentGroups(List<String> groups) {
        if (groups == null) {
            throw new IllegalArgumentException("parent groups list cannot be null");
        }
        parentGroups.clear();
        parentGroups.addAll(groups);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof NuxeoGroupImpl) {
            return name.equals(((NuxeoGroupImpl) other).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

}
