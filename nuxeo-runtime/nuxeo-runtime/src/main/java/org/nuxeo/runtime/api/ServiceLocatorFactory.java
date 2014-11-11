/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
