/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
@XObject("managedService")
public class ManagedServiceDescriptor implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 6338431911839779273L;

    @XNode("@name")
    protected String serviceName;

    @XNode("@class")
    protected String serviceClassName;

    @XNode("@iface")
    protected String ifaceClassName;

    @XNode("@isAdapted")
    protected boolean isAdapted;

    public String getServiceName() {
        if (serviceName == null) {
            return serviceName = serviceClassName;
        }
        return serviceName;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public String getIfaceClassName() {
        return ifaceClassName;
    }

    public String getName() {
        if (serviceName != null)
            return serviceName;
        return serviceClassName;
    }

    public boolean isAdapted() {
        return isAdapted;
    }

    @Override
    public String toString() {
        if (serviceName != null)
            return serviceName;
        return serviceClassName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ManagedServiceDescriptor other = (ManagedServiceDescriptor) obj;
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        return true;
    }

}
