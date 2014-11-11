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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.binding;

/**
 * A service provider is responsible to lookup services.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ServiceProvider {

    /**
     * Attaches the provider to a service manager.
     *
     * @param manager
     * @throws IllegalStateException if the provider is already attached to a manager
     */
    void setManager(ServiceManager manager);

    /**
     * Gets the service manager on which this provider is attached, or null if none.
     *
     * @return the service manager or null if none
     */
    ServiceManager getManager();

    /**
     * The provider is no more needed.
     * It should immediatley release any system resources.
     */
    void destroy();

    /**
     * Gets the service instance given its interface class. Returns null if no service is found
     * <p>
     * If the lookup succeeds and the provider is attached to a service manager it may use the binding key to
     * register a service binding on the manager so that the next time the same service is requested
     * it will be picked up from the manager cache.
     * <p>
     * Note that the binding key should be used to lookup the service.
     * Usually this key is the service class name but can be different when querying a named service
     * (in this case it will be serviceClass + '@' + serviceName).
     * <p>
     * If implementors doesn't support named services then they can use the serviceClass to
     * perform the lookup.
     *
     * @param serviceClass the interface of the service
     * @param bindingKey the binding key to use when caching bindings
     * @return the service instance if any was found or null otherwise
     */
    Object getService(Class<?> serviceClass, String bindingKey);

}
