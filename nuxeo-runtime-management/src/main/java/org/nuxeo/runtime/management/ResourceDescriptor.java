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
@XObject("resource")
public class ResourceDescriptor implements Serializable {

    private static final long serialVersionUID = 6338431911839779273L;

    protected ResourceDescriptor(String qualifiedName,
            Class<?> implClass) {
        this.name = qualifiedName;
        this.className = implClass.getCanonicalName();
        this.ifaceClassName = null;
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

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getIfaceClassName() {
        return ifaceClassName;
    }

    @Override
    public String toString() {
        if (name != null)
            return name;
        return className;
    }
}
