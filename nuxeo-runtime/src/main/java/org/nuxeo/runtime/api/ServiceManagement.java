/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServiceManagement extends DefaultComponent {

    private static final Log log = LogFactory.getLog(ServiceManagement.class);

    public static final ComponentName NAME = new ComponentName("org.nuxeo.runtime.api.ServiceManagement");

    private ServiceManager manager;

    @Override
    public void activate(ComponentContext context) throws Exception {
        manager = ServiceManager.getInstance();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        manager = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("servers")) {
            try {
                manager.registerServer((ServiceHost) contribution);
            } catch (Exception e) {
                log.error(e, e);
            }
        } else if (extensionPoint.equals("services")) {
            manager.registerService((ServiceDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("servers")) {
            manager.unregisterServer((ServiceHost) contribution);
        } else if (extensionPoint.equals("services")) {
            manager.unregisterService((ServiceDescriptor) contribution);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (ServiceManager.class.isAssignableFrom(adapter)) {
            return (T) manager;
        }
        return null;
    }

}
