/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
