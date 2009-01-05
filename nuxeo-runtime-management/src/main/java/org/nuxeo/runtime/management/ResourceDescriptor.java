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

import javax.management.ObjectName;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
@XObject("resource")
public class ResourceDescriptor implements Serializable {

    private static final long serialVersionUID = 6338431911839779273L;

    public ResourceDescriptor(ObjectName name, Class<?> implClass,
            Class<?> ifaceClass, boolean isAdapted) {
        this.name = name.getCanonicalName();
        this.className = implClass.getCanonicalName();
        this.ifaceClassName = ifaceClass.getCanonicalName();
        this.isAdapted = isAdapted;
    }
    
    public ResourceDescriptor(ObjectName name, Class<?> implClass) {
        this.name = name.getCanonicalName();
        this.className = implClass.getCanonicalName();
        this.ifaceClassName = null;
        this.isAdapted = true;
    }
    
    public ResourceDescriptor(ObjectName name, Class<?> implClass, boolean isAdapted) {
        this.name = name.getCanonicalName();
        this.className = implClass.getCanonicalName();
        this.ifaceClassName = null;
        this.isAdapted = isAdapted;
    }

    public ResourceDescriptor() {
        ;
    }

    @XNode("@name")
    private String name;

    @XNode("@class")
    private String className;

    @XNode("@iface")
    private String ifaceClassName;

    @XNode("@isAdapted")
    private boolean isAdapted;

    public String getName() {
        if (name == null) {
            return name = className;
        }
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getIfaceClassName() {
        return ifaceClassName;
    }

    public boolean isAdapted() {
        return isAdapted;
    }

    @Override
    public String toString() {
        if (name != null)
            return name;
        return className;
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
        ResourceDescriptor other = (ResourceDescriptor) obj;
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        return true;
    }

}
