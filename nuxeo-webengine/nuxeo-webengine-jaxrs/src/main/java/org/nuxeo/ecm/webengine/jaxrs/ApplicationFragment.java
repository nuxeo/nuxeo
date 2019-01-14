/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * A wrapper for a JAX-RS application fragment declared in manifest. The fragment application will be added to the
 * target host application.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ApplicationFragment extends Application {

    protected final String hostName;

    protected final Bundle bundle;

    protected final Map<String, String> attrs;

    protected String appClass;

    private volatile Application app;

    public Application getApplication() {
        return app;
    }

    public static Map<String, String> createAttributes(String hostName) {
        Map<String, String> attrs = new HashMap<>();
        if (hostName != null) {
            attrs.put("host", hostName);
        }
        return attrs;
    }

    public ApplicationFragment(Bundle bundle, String appClass) {
        this(bundle, appClass, (String) null);
    }

    public ApplicationFragment(Bundle bundle, String appClass, String host) {
        this(bundle, appClass, createAttributes(host));
    }

    public ApplicationFragment(Bundle bundle, String appClass, Map<String, String> attrs) {
        this.bundle = bundle;
        this.appClass = appClass;
        this.attrs = attrs;
        String host = attrs.get("host");
        this.hostName = host == null ? "default" : host;
    }

    protected synchronized void createApp() {
        try {
            Object obj = bundle.loadClass(appClass).getDeclaredConstructor().newInstance();
            if (obj instanceof ApplicationFactory) {
                app = ((ApplicationFactory) obj).getApplication(bundle, attrs);
            } else if (obj instanceof Application) {
                app = (Application) obj;
            } else {
                throw new IllegalArgumentException("Expecting an Application or ApplicationFactory class: " + appClass);
            }
        } catch (ReflectiveOperationException | IOException e) {
            String msg = "Cannot instantiate JAX-RS application " + appClass + " from bundle "
                    + bundle.getSymbolicName();
            throw new RuntimeException(msg, e);
        }
    }

    public synchronized void reload() {
        app = null;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public String getHostName() {
        return hostName;
    }

    public Application get() {
        if (app == null) {
            createApp();
        }
        return app;
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
