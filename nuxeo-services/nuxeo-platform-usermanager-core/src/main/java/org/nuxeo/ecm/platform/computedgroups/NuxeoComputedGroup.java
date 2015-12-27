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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.runtime.api.Framework;

/**
 * Computed group implementation class. Delegates part of the implementation logic to the {@link ComputedGroupsService}
 * that is pluggable.
 *
 * @author Thierry Delprat
 */
public class NuxeoComputedGroup implements ComputedGroup {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected List<String> parents;

    protected List<String> subGroups;

    protected List<String> members;

    private String label;

    public NuxeoComputedGroup(String name) {
        this(name, null);
    }

    public NuxeoComputedGroup(String name, String label) {
        this.name = name;
        this.label = label;
    }

    @Override
    public List<String> getMemberUsers() {
        if (members == null) {
            ComputedGroupsService cgs = Framework.getLocalService(ComputedGroupsService.class);
            if (cgs != null) {
                members = cgs.getComputedGroupMembers(name);
            }
            if (members == null) {
                members = new ArrayList<String>();
            }
        }
        return members;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLabel() {
        if (label != null) {
            return label;
        }
        return getName();
    }

    @Override
    public List<String> getParentGroups() {
        if (parents == null) {
            ComputedGroupsService cgs = Framework.getLocalService(ComputedGroupsService.class);
            if (cgs != null) {
                parents = cgs.getComputedGroupParent(name);
            }
            if (parents == null) {
                parents = new ArrayList<String>();
            }
        }
        return parents;
    }

    @Override
    public List<String> getMemberGroups() {
        if (subGroups == null) {
            ComputedGroupsService cgs = Framework.getLocalService(ComputedGroupsService.class);
            if (cgs != null) {
                subGroups = cgs.getComputedGroupSubGroups(name);
            }
            if (subGroups == null) {
                subGroups = new ArrayList<String>();
            }
        }
        return subGroups;
    }

    @Override
    public void setMemberGroups(List<String> groups) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setMemberUsers(List<String> users) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setLabel(String label) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

    @Override
    public void setParentGroups(List<String> groups) {
        throw new UnsupportedOperationException("Computed groups are read only");
    }

}
