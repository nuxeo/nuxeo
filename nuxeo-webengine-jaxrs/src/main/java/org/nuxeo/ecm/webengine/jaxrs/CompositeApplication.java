/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.jaxrs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.ws.rs.core.Application;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeApplication extends Application {

    protected List<Application> apps;

    public CompositeApplication() {
        apps = new Vector<Application>();
    }

    public void add(Application app) {
        apps.add(app);
    }

    public void remove(Application app) {
        apps.remove(app);
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> result = new HashSet<Class<?>>();
        for (Application app : apps) {
            result.addAll(app.getClasses());
        }
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> result = new HashSet<Object>();
        for (Application app : apps) {
            result.addAll(app.getSingletons());
        }
        return result;
    }

}
