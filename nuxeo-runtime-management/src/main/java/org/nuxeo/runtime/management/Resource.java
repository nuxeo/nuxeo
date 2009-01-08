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

import org.jsesoft.mmbi.NamedModelMBean;

/**
 * @authorStephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class Resource {

    protected final Object instance;

    protected final ObjectName managementName;

    protected final Class<?> clazz;

    protected NamedModelMBean mbean;

    /**
     * @param descriptor
     * @param managementName
     * @param managementClass
     * @param serviceInstance
     */
    public Resource(ObjectName managementName,
            Class<?> managementClass, Object serviceInstance) {
        this.managementName = managementName;
        this.clazz = managementClass;
        this.instance = serviceInstance;
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
