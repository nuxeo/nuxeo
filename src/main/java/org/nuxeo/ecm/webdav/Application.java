/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webdav;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webdav.provider.CoreSessionProvider;
import org.nuxeo.ecm.webdav.provider.ExceptionHandler;
import org.nuxeo.ecm.webdav.provider.WebDavContextResolver;
import org.nuxeo.ecm.webdav.resource.RootResource;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers the application (root resource classes and providers)
 * in a standard / container-neutral way.
 */
public class Application extends javax.ws.rs.core.Application {

    private static final Log log = LogFactory.getLog(Application.class);

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(RootResource.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(new ExceptionHandler());
        singletons.add(new CoreSessionProvider());
        singletons.add(new WebDavContextResolver());
        return singletons;
    }

}
