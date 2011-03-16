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

import java.net.URI;
import java.util.Hashtable;
import java.util.Map;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ServiceLocatorFactory {

    static final Map<String, ServiceLocatorFactory> factories = new Hashtable<String, ServiceLocatorFactory>();

    public abstract ServiceLocator createLocator(URI uri) throws Exception;


    public static void registerFactory(String type, ServiceLocatorFactory factory) {
        factories.put(type, factory);
    }

    public static void unregisterFactory(String type) {
        factories.remove(type);
    }

    public static ServiceLocatorFactory getFactory(String type) {
        return factories.get(type);
    }

    static {
        registerFactory("jboss", new JBossServiceLocatorFactory());
        //registerFactory("glassfish", new GlassfishServiceLocatorFactory()); // TODO
    }

}
