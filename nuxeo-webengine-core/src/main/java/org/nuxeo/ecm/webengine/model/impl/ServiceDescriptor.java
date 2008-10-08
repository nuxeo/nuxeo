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
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.model.Utils;
import org.nuxeo.ecm.webengine.model.WebService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("web-service")
public class ServiceDescriptor extends TypeDescriptor {

    @XNode("@class")
    void setClass(Class<?> clazz) { this.clazz = clazz; }
    
    @XNode("@name")
    void setName(String name) { this.name = name; }
    
    @XNode("@fragment")
    void setFragment(String fragment) { this.fragment = fragment; }
    
    @XNode("@superType")
    void setSuperType(String superType) { this.superType = superType; }
    
    @XNodeList(value="targetType", type=String[].class, componentType=String.class, nullByDefault=true)
    public String[] targetTypes;
    
    @XNodeList(value="facet", type=String[].class, componentType=String.class, nullByDefault=true)
    public String[] facets;
    
    
    /**
     * 
     */
    public ServiceDescriptor() {
        super ();
    }
    
    public ServiceDescriptor(Class<?> klass, String name, String superType) {
        super (klass, name, superType);
    }

    public ServiceDescriptor(Class<?> klass, String name, String superType, String[] types, String[] facets) {
        super (klass, name, superType);
        if (facets != null && facets.length > 0) {
            this.facets = facets;
        }
        if (types != null && types.length > 0) {
            this.targetTypes = types;
        }        
    }

    public boolean isService() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj instanceof ServiceDescriptor) {
            ServiceDescriptor td = (ServiceDescriptor)obj;
            return name.equals(td.name) && Utils.streq(fragment, td.fragment);
        }
        return false;
    }
    
    public String getId() {
        return name;
    }
    
    public String getFragment() {
        return fragment;
    }
    
    public boolean isMainFragment() {
        return fragment == null;
    }
    

    public static ServiceDescriptor fromAnnotation(Class<?> clazz, WebService type) {
        return  new ServiceDescriptor(clazz, type.name(), type.superType(), type.targetTypes(), type.facets());
    }
}
