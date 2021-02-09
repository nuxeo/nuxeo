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
 */
package org.nuxeo.ecm.core.security;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("item")
public class PermissionUIItemDescriptor {

    @XNode
    private String permission;

    @XNode("@show")
    private Boolean show;

    @XNode("@order")
    private Integer order;

    @XNode("@denyPermission")
    private String denyPermission;

    // needed by xmap
    public PermissionUIItemDescriptor() {
    }

    // needed by service API
    public PermissionUIItemDescriptor(PermissionUIItemDescriptor referenceDescriptor) {
        show = referenceDescriptor.show;
        order = referenceDescriptor.order;
        permission = referenceDescriptor.permission;
        denyPermission = referenceDescriptor.denyPermission;
    }

    public int getOrder() {
        return Objects.requireNonNullElse(order, 0);
    }

    public boolean isShown() {
        // permission items are shown by default
        return !Boolean.FALSE.equals(show);
    }

    public String getPermission() {
        return permission;
    }

    public String getDenyPermission() {
        return Objects.requireNonNullElse(denyPermission, permission);
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    public void merge(PermissionUIItemDescriptor pid) {
        // sanity check
        if (!permission.equals(pid.permission)) {
            throw new NuxeoException(
                    String.format("cannot merge permission item '%s' with '%s'", permission, pid.permission));
        }
        // do not merge unset attributes
        show = pid.show != null ? pid.show : show;
        order = pid.order != null ? pid.order : order;
        denyPermission = pid.denyPermission != null ? pid.denyPermission : denyPermission;
    }

    @Override
    public String toString() {
        if (denyPermission != null) {
            return String.format("PermissionUIItemDescriptor[%s (deny %s)]", permission, denyPermission);
        } else {
            return String.format("PermissionUIItemDescriptor[%s]", permission);
        }
    }

}
