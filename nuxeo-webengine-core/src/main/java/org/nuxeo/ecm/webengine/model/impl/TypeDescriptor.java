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
import org.nuxeo.ecm.webengine.model.ObjectType;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("type")
public class TypeDescriptor implements TypeDescriptorBase {

    @XNode("@class")
    public Class<?> clazz;
    
    @XNode("@name")
    public String name;
    
    @XNode("@fragment")
    public String fragment;
    
    @XNode("@superType")
    public String superType = ObjectType.ROOT_TYPE_NAME;
    
    
    /**
     * 
     */
    public TypeDescriptor() {
    }
    
    public TypeDescriptor(Class<?> klass, String name, String superType) {
        this.clazz = klass;
        this.name = name;
        this.superType = superType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj instanceof TypeDescriptor) {
            TypeDescriptor td = (TypeDescriptor)obj;
            return name.equals(td.name) && Utils.streq(fragment, td.fragment);
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
        return name;
    }
    
    public String getFragment() {
        return fragment;
    }
    
    public ActionTypeImpl asActionDescriptor() {
        return null;
    }
    
    public TypeDescriptor asTypeDescriptor() {
        return this;
    }
    
    public boolean isMainFragment() {
        return fragment == null;
    }
    

    public static TypeDescriptor fromAnnotation(Class<?> clazz, WebObject type) {
        return  new TypeDescriptor(clazz, type.name(), type.superType());
    }
}
