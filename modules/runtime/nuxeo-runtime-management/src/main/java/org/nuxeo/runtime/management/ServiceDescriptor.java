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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
@XObject("service")
public class ServiceDescriptor {

    protected ServiceDescriptor(String qualifiedName, Class<?> implClass) {
        name = qualifiedName;
        resourceClass = implClass;
        ifaceClass = null;
    }

    public ServiceDescriptor() {
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
        if (name != null) {
            return name;
        }
        return resourceClass.getCanonicalName();
    }

}
