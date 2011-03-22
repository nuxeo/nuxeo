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
 * $Id: PermissionUIItemDescriptor.java 28609 2008-01-09 16:38:30Z sfermigier $
 */

package org.nuxeo.ecm.core.security;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("item")
public class PermissionUIItemDescriptor implements Serializable{

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

    public PermissionUIItemDescriptor() {}

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

    public void merge(PermissionUIItemDescriptor pid) throws Exception {
        // sanity check
        if (!permission.equals(pid.permission)) {
            // TODO: use a dedicated Nuxeo Runtime / OSGi exception here
            throw new Exception(String.format(
                    "cannot merge permission item '%s' with '%s'", permission,
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
