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
