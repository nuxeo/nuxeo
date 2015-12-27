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
 * $Id: PermissionUIItemDescriptor.java 28609 2008-01-09 16:38:30Z sfermigier $
 */

package org.nuxeo.ecm.core.security;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("item")
public class PermissionUIItemDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@show")
    private Boolean show;

    @XNode("@order")
    private Integer order;

    @XNode("@denyPermission")
    private String denyPermission;

    @XNode("@id")
    private String id;

    private String permission = "";

    @XContent
    protected void setPermission(String permission) {
        this.permission = permission.trim();
    }

    public PermissionUIItemDescriptor() {
    }

    public PermissionUIItemDescriptor(PermissionUIItemDescriptor referenceDescriptor) {
        show = referenceDescriptor.show;
        order = referenceDescriptor.order;
        permission = referenceDescriptor.permission;
        denyPermission = referenceDescriptor.denyPermission;
        id = referenceDescriptor.id;
    }

    public int getOrder() {
        if (order == null) {
            // default order
            return 0;
        } else {
            return order;
        }
    }

    public boolean isShown() {
        if (show == null) {
            // permission items are shown by default
            return true;
        } else {
            return show;
        }
    }

    public String getPermission() {
        return permission;
    }

    public String getDenyPermission() {
        if (denyPermission != null) {
            return denyPermission;
        } else {
            return permission;
        }
    }

    public String getId() {
        if (id != null) {
            return id;
        } else {
            return permission;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PermissionUIItemDescriptor) {
            PermissionUIItemDescriptor otherPid = (PermissionUIItemDescriptor) other;
            if (!permission.equals(otherPid.permission)) {
                return false;
            }
            if (show != null) {
                if (!show.equals(otherPid.show)) {
                    return false;
                }
            } else {
                if (otherPid.show != null) {
                    return false;
                }
            }
            if (order != null) {
                if (!order.equals(otherPid.order)) {
                    return false;
                }
            } else {
                if (otherPid.order != null) {
                    return false;
                }
            }
            if (getId() != null) {
                if (!getId().equals(otherPid.getId())) {
                    return false;
                }
            } else {
                if (otherPid.getId() != null) {
                    return false;
                }
            }
            if (getDenyPermission() != null) {
                if (!getDenyPermission().equals(otherPid.getDenyPermission())) {
                    return false;
                }
            } else {
                if (otherPid.getDenyPermission() != null) {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    public void merge(PermissionUIItemDescriptor pid) {
        // sanity check
        if (!permission.equals(pid.permission)) {
            throw new NuxeoException(String.format("cannot merge permission item '%s' with '%s'", permission,
                    pid.permission));
        }
        // do not merge unset attributes
        show = pid.show != null ? pid.show : show;
        order = pid.order != null ? pid.order : order;
        id = pid.id != null ? pid.id : id;
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
