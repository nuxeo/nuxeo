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

package org.nuxeo.runtime.osgi;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.errors.CompoundIOExceptionBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeModelException;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.AbstractRuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiRuntimeContext extends AbstractRuntimeContext {

    protected final Bundle bundle;

    protected final Log log = LogFactory.getLog(OSGiRuntimeContext.class);

    protected String hostBundleId;

    protected Bundle hostBundle;

    public OSGiRuntimeContext(Bundle bundle) {
        super(bundle.getSymbolicName());
        this.bundle = bundle;
        // workaround to correctly handle fragment class loaders
        hostBundleId = bundle.getHeaders().get(Constants.FRAGMENT_HOST);
        if (hostBundleId != null) {
            int p = hostBundleId.indexOf(';');
            if (p > -1) { // remove version or other extra information if any
                hostBundleId = hostBundleId.substring(0, p);
            }
        }
    }

    @Override
    protected void handleRegistering() throws RuntimeModelException {
        super.handleRegistering();
        try {
            loadComponents();
        } catch (IOException e) {
            throw new RuntimeModelException(this + " : cannot load components", e);
        }
    }

    protected void loadComponents() throws IOException {
        Bundle bundle = getBundle();
        String list = OSGiRuntimeService.getComponentsList(bundle);
        LogFactory.getLog(OSGiRuntimeContext.class).debug(
                "Bundle: " + name + " components: " + list);
        if (list == null) {
            return;
        }
        StringTokenizer tok = new StringTokenizer(list, ", \t\n\r\f");
        CompoundIOExceptionBuilder errors = new CompoundIOExceptionBuilder();
        while (tok.hasMoreTokens()) {
            String path = tok.nextToken();
            URL url = bundle.getEntry(path);
            log.debug("Loading component for: " + name + " path: " + path
                    + " url: " + url);
            if (url != null) {
                try {
                    deploy(url);
                } catch (RuntimeModelException e) {
                    errors.add(new IOException("Error deploying resource: " + url, e));
                }
            } else {
                errors.add(new IOException("Unknown component '" + path
                        + "' referenced by bundle '" + name + "'" + ". Check the MANIFEST.MF"));
            }
        }
        errors.throwOnError();
    }

    protected Bundle[] requiredBundles = new Bundle[0];

    @Override
    protected void handleResolved() {
        super.handleResolved();
        Set<Bundle> bundles = new HashSet<Bundle>(requiredContexts.size());
        for (RuntimeContext context:requiredContexts) {
            bundles.add(((OSGiRuntimeContext)context).bundle);
        }
        requiredBundles = bundles.toArray(new Bundle[bundles.size()]);
    }

    public Bundle[] getRequiredBundles() {
        return requiredBundles;
    }

    @Override
    protected void handleActivating() {
        reader.flushDeferred();
        super.handleActivating();
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public URL getResource(String name) {
        URL url = null;
        if (hostBundleId != null) {
            url = getHostBundle().getResource(name);
        } else {
            url = bundle.getResource(name);
        }
        if (url == null) {
            url = Framework.getResourceLoader().getResource(name);
        }
        return url;
    }

    @Override
    public URL getLocalResource(String name) {
        URL url = null;
        if (hostBundleId != null) {
            url = getHostBundle().getEntry(name);
        } else {
            url = bundle.getEntry(name);
        }
        if (url == null) {
            url = Framework.getResourceLoader().getResource(name);
        }
        return url;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            if (hostBundleId != null) { // workaround to handle fragment bundles
                                        // that doesn't have class loaders
                return getHostBundle().loadClass(className);
            }
            return bundle.loadClass(className);
        } catch (ClassNotFoundException e) {
            return Framework.getResourceLoader().loadClass(className);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return bundle.adapt(ClassLoader.class);
    }

    public Bundle getHostBundle() {
        if (hostBundleId != null) {
            if (hostBundle == null && runtime instanceof OSGiRuntimeService) {
                hostBundle = ((OSGiRuntimeService) runtime).findHostBundle(bundle);
            }
        }
        return hostBundle;
    }

    @Override
    public String toString() {
        return "OSGiRuntimeContext [bundle=" + bundle + ", state=" + state
                + "]";
    }


}
