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
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.Utils;
import org.nuxeo.ecm.webengine.model.WebAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("web-adapter")
public class AdapterDescriptor extends TypeDescriptor {

    @XNode("@class")
    void setClass(Class<?> clazz) {
        this.clazz = new StaticClassProxy(clazz);
    }

    @XNode("@type")
    void setType(String type) {
        this.type = type;
    }

    @XNode("@name")
    public String name;

    @XNode("@fragment")
    void setFragment(String fragment) {
        this.fragment = fragment;
    }

    @XNode("@superType")
    void setSuperType(String superType) {
        this.superType = superType;
    }

    @XNode(value = "targetType")
    public String targetType = ResourceType.ROOT_TYPE_NAME;

    @XNodeList(value = "facet", type = String[].class, componentType = String.class, nullByDefault = true)
    public String[] facets;

    public AdapterDescriptor() {
    }

    public AdapterDescriptor(ClassProxy clazz, String name, String type, String superType) {
        super(clazz, type, superType);
        this.name = name;
    }

    public AdapterDescriptor(ClassProxy clazz, String name, String type, String superType, String targetType,
            String[] facets) {
        super(clazz, type, superType);
        this.name = name;
        if (facets != null && facets.length > 0) {
            this.facets = facets;
        }
        if (targetType == null || "*".equals(targetType)) {
            this.targetType = ResourceType.ROOT_TYPE_NAME;
        } else {
            this.targetType = targetType;
        }
    }

    @Override
    public boolean isAdapter() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == AdapterDescriptor.class) { // don't use instanceof in an overridden equals() method
            AdapterDescriptor td = (AdapterDescriptor) obj;
            return type.equals(td.type) && Utils.streq(fragment, td.fragment);
        }
        return false;
    }

    @Override
    public String getId() {
        return type;
    }

    @Override
    public String getFragment() {
        return fragment;
    }

    @Override
    public boolean isMainFragment() {
        return fragment == null;
    }

    @Override
    public AdapterDescriptor asAdapterDescriptor() {
        return this;
    }

    public static AdapterDescriptor fromAnnotation(ClassProxy clazz, WebAdapter type) {
        return new AdapterDescriptor(clazz, type.name(), type.type(), type.superType(), type.targetType(),
                type.facets());
    }

}
