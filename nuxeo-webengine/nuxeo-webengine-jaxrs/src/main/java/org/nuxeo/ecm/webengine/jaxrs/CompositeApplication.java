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
 */
package org.nuxeo.ecm.webengine.jaxrs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.jaxrs.views.ViewMessageBodyWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeApplication extends Application {

    protected final List<ApplicationProxy> apps;

    public CompositeApplication() {
        apps = new ArrayList<ApplicationProxy>();
    }

    public synchronized void add(ApplicationProxy app) {
        apps.add(app);
    }

    public synchronized void remove(ApplicationProxy app) {
        apps.remove(app);
    }

    public synchronized ApplicationProxy[] getApplications() {
        return apps.toArray(new ApplicationProxy[apps.size()]);
    }

    public synchronized void reload() {
        for (ApplicationProxy proxy : getApplications()) {
            proxy.reset();
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> result = new HashSet<Class<?>>();
        for (Application app : getApplications()) {
            result.addAll(app.getClasses());
        }
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> result = new HashSet<Object>();
        result.add(new ViewMessageBodyWriter());
        for (Application app : getApplications()) {
            result.addAll(app.getSingletons());
        }
        return result;
    }

}
