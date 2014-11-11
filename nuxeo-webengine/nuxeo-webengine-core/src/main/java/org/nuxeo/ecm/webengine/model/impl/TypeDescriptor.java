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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;


import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.model.Private;
import org.nuxeo.ecm.webengine.model.Protected;
import org.nuxeo.ecm.webengine.model.Public;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.TypeVisibility;
import org.nuxeo.ecm.webengine.model.Utils;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("web-object")
public class TypeDescriptor implements Cloneable {

    @XNode("@class")
    void setClassProxy(Class<?> clazz) {
        this.clazz = new StaticClassProxy(clazz);
    }
    public ClassProxy clazz;

    @XNode("@name")
    public String type;

    @XNode("@fragment")
    public String fragment;

    @XNode("@superType")
    public String superType = ResourceType.ROOT_TYPE_NAME;

    @XNode("@visibility")
    public void setVisibility(String v) {
        if (v.equals("public")) {
            visibility = TypeVisibility.PUBLIC;
        } else if (v.equals("protected")) {
            visibility = TypeVisibility.PROTECTED;
        } else if (v.equals("private")) {
            visibility = TypeVisibility.PRIVATE;
        } else {
            visibility = TypeVisibility.DEFAULT;
        }
    }
    public int visibility = TypeVisibility.DEFAULT;

    public TypeDescriptor() {
    }

    public TypeDescriptor(ClassProxy clazz, String type, String superType) {
        this.clazz = clazz;
        this.type = type;
        this.superType = superType;
        Class<?> k = clazz.get();
        if (k.isAnnotationPresent(Public.class)) {
            visibility = TypeVisibility.PUBLIC;
        } else if (k.isAnnotationPresent(Protected.class)) {
            visibility = TypeVisibility.PROTECTED;
        } else if (k.isAnnotationPresent(Private.class)) {
            visibility = TypeVisibility.PRIVATE;
        }
    }

    public int getVisibility() {
        return visibility;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof TypeDescriptor) {
            TypeDescriptor td = (TypeDescriptor) obj;
            return type.equals(td.type) && Utils.streq(fragment, td.fragment);
        }
        return false;
    }

    @Override
    public TypeDescriptor clone() {
        try {
            return (TypeDescriptor)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error("Canot happen");
        }
    }

    public String getId() {
        return type;
    }

    public String getFragment() {
        return fragment;
    }

    public boolean isMainFragment() {
        return fragment == null;
    }

    public boolean isAdapter() {
        return false;
    }

    public AdapterDescriptor asAdapterDescriptor() {
        return null;
    }

    public TypeDescriptor asTypeDescriptor() {
        return this;
    }

    public static TypeDescriptor fromAnnotation(ClassProxy clazz, WebObject type) {
        return new TypeDescriptor(clazz, type.type(), type.superType());
    }

    @Override
    public String toString() {
        return type+ " extends "+superType+" ["+clazz.getClassName()+"]";
    }

}
