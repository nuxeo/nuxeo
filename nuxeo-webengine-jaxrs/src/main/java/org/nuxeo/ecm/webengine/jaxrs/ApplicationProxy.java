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

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * A wrapper for the JAX-RS application declared in manifest.
 * This is a proxy to the real application implementation which will be created in a lazy fashion,
 * at the first call.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationProxy extends Application {

    protected final Bundle bundle;

    protected final String className;

    protected final Map<String,String> attrs;

    protected volatile Application delegate;


    public ApplicationProxy(Bundle bundle, String className, Map<String,String> attrs) {
        this.bundle = bundle;
        this.className = className;
        this.attrs = attrs;
    }

    public String getClassName() {
        return className;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void reset() {
        delegate = null;
    }

    public Application get() {
        if (delegate == null) {
            try {
                Object obj = bundle.loadClass(className).newInstance();
                if (obj instanceof ApplicationFactory) {
                    delegate = ((ApplicationFactory)obj).getApplication(bundle, attrs);
                } else if (obj instanceof Application) {
                    delegate = (Application)obj;
                } else {
                    throw new IllegalArgumentException("Expecting an Application or ApplicationFactory class: "+className);
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate JAX-RS application "+className+" from bundle "+bundle.getSymbolicName(), e);
            }
        }
        return delegate;
    }


    @Override
    public Set<Class<?>> getClasses() {
        return get().getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return get().getSingletons();
    }

}
