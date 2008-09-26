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

package org.nuxeo.ecm.webengine.rest.impl;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.rest.annotations.Type;
import org.nuxeo.ecm.webengine.rest.model.WebType;

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
    public String superType = WebType.ROOT_TYPE_NAME;
    
    @XNodeMap(value="action", key="action@name", type=HashMap.class, componentType=ActionDescriptor.class)
    public Map<String, ActionDescriptor> actions; 
   
    
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
            return name.equals(td.name) && streq(fragment, td.fragment);
        }
        return false;
    }
    
    @Override
    public TypeDescriptor clone() {
        try {
            TypeDescriptor td  = (TypeDescriptor)super.clone();
            td.actions = new HashMap<String, ActionDescriptor>(actions); 
            return td;
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
    
    public ActionDescriptor asActionDescriptor() {
        return null;
    }
    
    public TypeDescriptor asTypeDescriptor() {
        return this;
    }
    
    public boolean isMainFragment() {
        return fragment == null;
    }
    
    public final static boolean streq(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else if (str2 == null) {
            return str1 == null;
        } else {
            return str1.equals(str2);
        }
    }

    public static TypeDescriptor fromAnnotation(Class<?> clazz, Type type) {
        return  new TypeDescriptor(clazz, type.value(), type.superType());
    }
}
