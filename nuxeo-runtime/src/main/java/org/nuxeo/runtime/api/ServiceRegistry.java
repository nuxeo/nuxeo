/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
