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
 *     matic
 */
package org.nuxeo.runtime.management;

import javax.management.ObjectName;

/**
 * @author matic
 * 
 */
public class ManagedService {

    protected final Object serviceInstance;

    protected final ObjectName managementName;

    protected final Class<?> managementClass;

    protected final ManagedServiceDescriptor descriptor;


    /**
     * @param descriptor
     * @param managementName
     * @param managementClass
     * @param serviceInstance
     */
    public ManagedService(ManagedServiceDescriptor descriptor,
            ObjectName managementName, Class<?> managementClass,
            Object serviceInstance) {
        this.descriptor = descriptor;
        this.managementName = managementName;
        this.managementClass = managementClass;
        this.serviceInstance = serviceInstance;
    }

    public Object getServiceInstance() {
        return serviceInstance;
    }

    public ObjectName getManagementName() {
        return managementName;
    }

    public Class<?> getManagementClass() {
        return managementClass;
    }

    public ManagedServiceDescriptor getDescriptor() {
        return descriptor;
    }
}
