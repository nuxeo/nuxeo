/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

/**
 * A service provider.
 * <p>
 * A service provider is used by the framework to be able to change the way local services are found.
 * <p>
 * For example you may want to use a simple service provider for testing pourpose to avoid loading the nuxeo runtime
 * framework to register services.
 * <p>
 * To set a service provider use: {@link DefaultServiceProvider#setProvider(ServiceProvider)}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ServiceProvider {

    /**
     * Gets the service instance given its API class.
     */
    <T> T getService(Class<T> serviceClass);

}
