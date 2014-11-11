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
