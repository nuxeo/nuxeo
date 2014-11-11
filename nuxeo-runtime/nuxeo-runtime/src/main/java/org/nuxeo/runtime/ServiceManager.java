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

package org.nuxeo.runtime;

/**
 * A service manager.
 * This interface was created to be able to plug different service managers in Framework.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ServiceManager {

    /**
     * Gets a service implementation given the interface class.
     *
     * @param <T>
     * @param serviceClass the service interface class
     * @return the implementation
     * @throws Exception
     */
    <T> T getService(Class<T> serviceClass) throws Exception;

    /**
     * Gets a service implementation given the interface class and a name.
     * <p>
     * This is useful to lookup services that are not singletons and can be identified
     * using a service name.
     *
     * @param <T>
     * @param serviceClass the service interface class
     * @param name the service name
     * @return the implementation
     * @throws Exception
     */
    <T> T getService(Class<T> serviceClass, String name)  throws Exception;

}
