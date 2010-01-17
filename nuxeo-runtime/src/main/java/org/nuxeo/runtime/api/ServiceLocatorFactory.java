/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
