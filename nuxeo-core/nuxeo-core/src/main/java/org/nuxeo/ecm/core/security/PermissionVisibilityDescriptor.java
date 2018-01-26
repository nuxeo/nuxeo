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
 * $Id: PermissionVisibilityDescriptor.java 28609 2008-01-09 16:38:30Z sfermigier $
 */

package org.nuxeo.ecm.core.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;

@XObject("visibility")
public class PermissionVisibilityDescriptor {

    @XNode("@type")
    private String typeName = "";

    private final List<PermissionUIItemDescriptor> items = new CopyOnWriteArrayList<>();

    private String[] sortedPermissionNames;

    public PermissionVisibilityDescriptor() {
    }

    public PermissionVisibilityDescriptor(PermissionVisibilityDescriptor pvd) {
        typeName = pvd.typeName;
        for (PermissionUIItemDescriptor pid : pvd.items) {
            items.add(new PermissionUIItemDescriptor(pid));
        }
    }

    @XNodeList(value = "item", type = PermissionUIItemDescriptor[].class, componentType = PermissionUIItemDescriptor.class)
    protected void setPermissionUIItems(PermissionUIItemDescriptor[] items) {
        this.items.clear();
        this.items.addAll(Arrays.asList(items));
        sortedPermissionNames = null;
    }

    public String getTypeName() {
        return typeName;
    }

    public List<PermissionUIItemDescriptor> getPermissionUIItems() {
        return items;
    }

    public void merge(PermissionVisibilityDescriptor other) {
        List<PermissionUIItemDescriptor> otherItems = new ArrayList<PermissionUIItemDescriptor>(other.items);
        List<PermissionUIItemDescriptor> mergedItems = new LinkedList<PermissionUIItemDescriptor>();

        // merge items with common permission names
        for (PermissionUIItemDescriptor item : items) {
            for (PermissionUIItemDescriptor otherItem : otherItems) {
                if (item.getPermission().equals(otherItem.getPermission())) {
                    item.merge(otherItem);
                    mergedItems.add(otherItem);
                }
            }
            otherItems.removeAll(mergedItems);
            mergedItems.clear();
        }
        // add items for new permission names
        items.addAll(otherItems);
        sortedPermissionNames = null;
    }

    public String[] getSortedItems() {
        if (sortedPermissionNames == null) {
            Collections.sort(items, new PermissionUIItemComparator());
            List<String> filteredPermissions = new LinkedList<String>();
            for (PermissionUIItemDescriptor pid : items) {
                if (pid.isShown()) {
                    filteredPermissions.add(pid.getPermission());
                }
            }
            sortedPermissionNames = filteredPermissions.toArray(new String[filteredPermissions.size()]);
        }
        return sortedPermissionNames;
    }

    public List<UserVisiblePermission> getSortedUIPermissionDescriptor() {
        Collections.sort(items, new PermissionUIItemComparator());
        List<UserVisiblePermission> result = new ArrayList<UserVisiblePermission>();
        for (PermissionUIItemDescriptor pid : items) {
            if (pid.isShown()) {
                result.add(new UserVisiblePermission(pid.getId(), pid.getPermission(), pid.getDenyPermission()));
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PermissionVisibilityDescriptor) {
            PermissionVisibilityDescriptor otherPvd = (PermissionVisibilityDescriptor) other;
            if (!typeName.equals(otherPvd.typeName)) {
                return false;
            }
            if (!items.equals(otherPvd.items)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("PermissionVisibilityDescriptor[%s]", typeName);
    }

}
