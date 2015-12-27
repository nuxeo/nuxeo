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

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoGroup;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class NuxeoGroupImpl implements NuxeoGroup {

    private static final long serialVersionUID = -69828664399387083L;

    private final List<String> users;

    private final List<String> groups;

    private final List<String> parentGroups;

    private String name;

    private String label;

    public NuxeoGroupImpl(String name) {
        if (name == null) {
            throw new IllegalArgumentException("group name cannot be null");
        }
        this.name = name.trim();
        label = name.trim();
        users = new ArrayList<String>();
        groups = new ArrayList<String>();
        parentGroups = new ArrayList<String>();
    }

    public NuxeoGroupImpl(String name, String label) {
        this(name);
        this.label = label;
    }

    @Override
    public List<String> getMemberUsers() {
        return users;
    }

    @Override
    public List<String> getMemberGroups() {
        return groups;
    }

    @Override
    public List<String> getParentGroups() {
        return parentGroups;
    }

    @Override
    public void setMemberUsers(List<String> users) {
        if (users == null) {
            throw new IllegalArgumentException("member users list cannot be null");
        }
        this.users.clear();
        this.users.addAll(users);
    }

    @Override
    public void setMemberGroups(List<String> groups) {
        if (groups == null) {
            throw new IllegalArgumentException("member groups list cannot be null");
        }
        this.groups.clear();
        this.groups.addAll(groups);
    }

    @Override
    public void setParentGroups(List<String> groups) {
        if (groups == null) {
            throw new IllegalArgumentException("parent groups list cannot be null");
        }
        parentGroups.clear();
        parentGroups.addAll(groups);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
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
