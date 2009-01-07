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

    protected ResourceDescriptor(String shortName, String qualifiedName,
            Class<?> implClass) {
        this.shortName = shortName;
        this.qualifiedName = qualifiedName;
        this.className = implClass.getCanonicalName();
        this.ifaceClassName = null;
        this.isAdapted = true;
    }

    public ResourceDescriptor() {
        ;
    }

    @XNode("@name")
    private String shortName;

    @XNode("@name")
    private String qualifiedName;
    
    @XNode("@kind")
    private String kind;

    @XNode("@class")
    private String className;

    @XNode("@iface")
    private String ifaceClassName;

    @XNode("@isAdapted")
    private boolean isAdapted;

    public String getShortName() {
        return shortName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getKind() {
        return kind;
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
        if (shortName != null)
            return shortName;
        if (qualifiedName != null) 
            return qualifiedName;
        return className;
    }
}
