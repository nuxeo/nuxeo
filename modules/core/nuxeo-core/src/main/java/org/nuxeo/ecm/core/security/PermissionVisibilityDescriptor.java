/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.core.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;

@XObject("visibility")
@XRegistry
public class PermissionVisibilityDescriptor {

    /** @since 11.5 **/
    public static final Comparator<PermissionUIItemDescriptor> ITEM_COMPARATOR = Comparator.comparing(
            PermissionUIItemDescriptor::getOrder).thenComparing(PermissionUIItemDescriptor::getPermission);

    @XNode(value = "@type", defaultAssignment = PermissionVisibilityRegistry.DEFAULT_ID)
    @XRegistryId
    private String typeName = PermissionVisibilityRegistry.DEFAULT_ID;

    @XNodeList(value = "item", type = ArrayList.class, componentType = PermissionUIItemDescriptor.class)
    private List<PermissionUIItemDescriptor> items = new ArrayList<>();

    // needed by xmap
    public PermissionVisibilityDescriptor() {
    }

    // needed by service API
    public PermissionVisibilityDescriptor(PermissionVisibilityDescriptor pvd) {
        typeName = pvd.typeName;
        for (PermissionUIItemDescriptor pid : pvd.items) {
            items.add(new PermissionUIItemDescriptor(pid));
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public List<PermissionUIItemDescriptor> getPermissionUIItems() {
        return Collections.unmodifiableList(items);
    }

    public void merge(PermissionVisibilityDescriptor other) {
        List<PermissionUIItemDescriptor> otherItems = new ArrayList<>(other.items);
        List<PermissionUIItemDescriptor> mergedItems = new LinkedList<>();

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
    }

    public String[] getSortedItems() {
        return items.stream()
                    .filter(PermissionUIItemDescriptor::isShown)
                    .sorted(ITEM_COMPARATOR)
                    .map(PermissionUIItemDescriptor::getPermission)
                    .toArray(String[]::new);
    }

    public List<UserVisiblePermission> getSortedUIPermissionDescriptor() {
        return items.stream()
                    .filter(PermissionUIItemDescriptor::isShown)
                    .sorted(ITEM_COMPARATOR)
                    .map(pid -> new UserVisiblePermission(pid.getPermission(), pid.getDenyPermission()))
                    .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public String toString() {
        return String.format("PermissionVisibilityDescriptor[%s]", typeName);
    }

}
