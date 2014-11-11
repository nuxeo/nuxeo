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
 *    Stephane Lacoin (Nuxeo EP Software Engineer)ne Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import javax.management.ObjectName;
import javax.management.modelmbean.RequiredModelMBean;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 *
 */
public class Resource {

    protected final Object instance;

    protected final ObjectName managementName;

    protected final Class<?> clazz;

    protected RequiredModelMBean mbean;

    public Resource(ObjectName managementName,
            Class<?> managementClass, Object serviceInstance) {
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
