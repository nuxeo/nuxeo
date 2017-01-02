/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((fragment == null) ? 0 : fragment.hashCode());
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
        if (!(obj instanceof TypeDescriptor)) {
            return false;
        }
        TypeDescriptor other = (TypeDescriptor) obj;
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (fragment == null) {
            if (other.fragment != null) {
                return false;
            }
        } else if (!fragment.equals(other.fragment)) {
            return false;
        }
        return true;
    }

    @Override
    public TypeDescriptor clone() {
        try {
            return (TypeDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Canot happen");
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
        return type + " extends " + superType + " [" + clazz.getClassName() + "]";
    }

}
