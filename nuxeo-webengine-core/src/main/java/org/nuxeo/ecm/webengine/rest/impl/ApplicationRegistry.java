/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rest.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.RootDescriptor;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationRegistry extends AbstractContributionRegistry<String, ApplicationDescriptor> {

    protected Map<String, WebApplication> apps = new ConcurrentHashMap<String, WebApplication>();

    protected WebEngine2 engine;


    public ApplicationRegistry(WebEngine2 engine) {
        this.engine = engine;
    }

    public WebEngine2 getEngine() {
        return engine;
    }

    public void putApplication(WebApplication app) {
        apps.put(app.getName(), app);
    }

    public WebApplication removeApplication(String id) {
        return apps.remove(id);
    }

    public WebApplication getApplication(String id) {
        return apps.get(id);
    }

    public WebApplication[] getApplications() {
        return apps.values().toArray(new WebApplication[apps.size()]);
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
        apps.clear();
    }

    protected ApplicationDescriptor clone(ApplicationDescriptor descriptor) {
        return descriptor.clone();
    }
    
    @Override
    protected void applyFragment(ApplicationDescriptor object, ApplicationDescriptor fragment) {
        if (fragment.defaultPage != "default.ftl") {
            object.defaultPage = fragment.defaultPage;
        }
        if (fragment.errorPage != "error.ftl") {
            object.errorPage = fragment.errorPage;
        }
        if (fragment.indexPage != "index.ftl") {
            object.indexPage = fragment.indexPage;
        }
        if (fragment.guardDescriptor != null) {
            object.guardDescriptor = fragment.guardDescriptor;
        }
        if (fragment.roots != null) {
            if (object.roots == null) {
                object.roots = new ArrayList<RootDescriptor>();
            }
            object.roots.addAll(fragment.roots);
        }
    }

    @Override
    protected void installContribution(String key, ApplicationDescriptor object) {
        try {
            WebApplication app = new WebApplicationImpl(engine, object.directory, object);
            apps.put(key, app);
        } catch (Exception e) {
            e.printStackTrace(); //TODO
        }
    }

    @Override
    protected void updateContribution(String key, ApplicationDescriptor object) {
        installContribution(key, object);
    }

    @Override
    protected void uninstallContribution(String key) {
        apps.remove(key);
    }
    
    @Override
    protected boolean isMainFragment(ApplicationDescriptor object) {
        return object.fragment == null || object.fragment.length() == 0;
    }

    public void registerDescriptor(File root, ApplicationDescriptor desc) {
        desc.directory = root;
        addFragment(desc.name, desc, desc.base);
    }

    public void unregisterDescriptor(ApplicationDescriptor desc) {
        removeFragment(desc.name, desc);
    }

}
