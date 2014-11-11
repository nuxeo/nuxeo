/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.api;

/**
 * A service provider.
 * <p>
 * A service provider is used by the framework to be able to change the way
 * local services are found.
 * <p>
 * For example you may want to use a simple service provider for testing pourpose to avoid loading
 * the nuxeo runtime framework to register services.
 * <p>
 * To set a service provider use:
 * {@link DefaultServiceProvider#setProvider(ServiceProvider)}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ServiceProvider {

    /**
     * Gets the service instance given its API class.
     */
    <T> T getService(Class<T> serviceClass);

}
