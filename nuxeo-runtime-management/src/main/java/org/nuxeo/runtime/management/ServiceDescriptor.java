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
@XObject("service")
public class ServiceDescriptor implements Serializable {

    private static final long serialVersionUID = 6338431911839779273L;

    protected ServiceDescriptor(String qualifiedName,
            Class<?> implClass) {
        this.name = qualifiedName;
        this.resourceClass = implClass;
        this.ifaceClass = null;
    }

    public ServiceDescriptor() {
        ;
    }

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<?> resourceClass;


    @XNode("@iface")
    private Class<?> ifaceClass;

    public String getName() {
        return name;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }

    public Class<?> getIfaceClass() {
        return ifaceClass;
    }

    @Override
    public String toString() {
        if (name != null)
            return name;
        return resourceClass.getCanonicalName();
    }
}
