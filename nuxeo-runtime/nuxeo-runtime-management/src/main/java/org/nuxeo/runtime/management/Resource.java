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
 *    Stephane Lacoin (Nuxeo EP Software Engineer)ne Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import javax.management.ObjectName;
import javax.management.modelmbean.RequiredModelMBean;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class Resource {

    protected final Object instance;

    protected final ObjectName managementName;

    protected final Class<?> clazz;

    protected RequiredModelMBean mbean;

    public Resource(ObjectName managementName, Class<?> managementClass, Object serviceInstance) {
        this.managementName = managementName;
        clazz = managementClass;
        instance = serviceInstance;
    }

    public Object getInstance() {
        return instance;
    }

    public ObjectName getManagementName() {
        return managementName;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public boolean isRegistered() {
        return mbean != null;
    }

}
