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
package org.nuxeo.ecm.webengine.app;

import java.io.File;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.WebEngine;
import org.osgi.framework.Bundle;

/**
 * A JAX-RS application deployed from a bundle.
 * <p>
 * This is a wrapper of the original application specified by the used in the
 * the bundle MANIFEST. A bundle may deploy at most one application. A bundled
 * application is uniquely identified by the type name of the wrapped
 * application class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BundledApplication extends Application {

    /**
     * The JAX-RS application deployed from the bundle
     */
    protected Application app;

    /**
     * The bundle declaring the application
     */
    protected Bundle bundle;

    /**
     * Hash code cache
     */
    protected int hash = 0;

    public BundledApplication(Bundle bundle, Application app) {
        this.bundle = bundle;
        this.app = app;
    }

    /**
     * Reloads the application class in the context of web engine loader.
     */
    public void reload(WebEngine engine) throws Exception {
        if (isWebApplication()) {
            File dir = ((WebApplication) app).getConfiguration().getDirectory();
            reInstantiate(engine);
            if (isWebApplication()) {
                ((WebApplication) app).setModuleDirectory(dir);
            }
        } else {
            reInstantiate(engine);
        }
    }

    protected void reInstantiate(WebEngine engine) throws Exception {
        app = (Application) engine.loadClass(app.getClass().getName()).newInstance();
    }

    /**
     * The application ID. This is the same as the bundle symbolic name owning
     * the application.
     */
    public String getId() {
        return app.getClass().getName();
    }

    public boolean isWebEngineModule() {
        return app instanceof WebEngineModule;
    }

    public boolean isWebApplication() {
        return app instanceof WebApplication;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Application getApplication() {
        return app;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return app.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return app.getSingletons();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BundledApplication) {
            return ((BundledApplication) obj).getId().equals(getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = getId().hashCode();
            hash = h;
        }
        return h;
    }

}
