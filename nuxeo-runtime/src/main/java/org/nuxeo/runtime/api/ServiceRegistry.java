/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Hashtable;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServiceRegistry {

    private final Map<String, ServiceDescriptor> services = new Hashtable<String, ServiceDescriptor>();

    public void addService(ServiceDescriptor sd) {
        services.put(sd.getInstanceName(), sd);
    }

    public void removeService(String instanceName) {
        services.remove(instanceName);
    }

    public ServiceDescriptor getService(String instanceName) {
        return services.get(instanceName);
    }

    public void clear() {
        services.clear();
    }

}
