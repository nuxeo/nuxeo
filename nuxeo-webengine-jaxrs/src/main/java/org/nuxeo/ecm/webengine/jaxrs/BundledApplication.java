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

import java.util.Set;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * A wrapper for the JAX-RS application declared in manifest
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundledApplication extends Application {

    protected Bundle bundle;

    protected String className;

    protected volatile Application delegate;


    public BundledApplication(Bundle bundle, String className) {
        this.bundle = bundle;
        this.className = className;
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

    public Application getDelegate() {
        if (delegate == null) {
            try {
                delegate = (Application)bundle.loadClass(className).newInstance();
            } catch (ClassCastException e) {
                throw new RuntimeException("JAX-RS application classes must extends "+Application.class.getName()+". Faulty class: "+className+" in bundle "+bundle.getSymbolicName());
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate JAX-RS application "+className+" from bundle "+bundle.getSymbolicName());
            }
        }
        return delegate;
    }


    @Override
    public Set<Class<?>> getClasses() {
        return getDelegate().getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return getDelegate().getSingletons();
    }

}
