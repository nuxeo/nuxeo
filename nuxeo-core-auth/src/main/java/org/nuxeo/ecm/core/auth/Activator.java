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
package org.nuxeo.ecm.core.auth;

import java.io.File;

import org.nuxeo.common.Environment;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Activator implements BundleActivator {

    protected static Activator instance;

    public static Activator getInstance() {
        return instance;
    }

    protected SimpleUserRegistry registry;

    public SimpleUserRegistry getRegistry() {
        return registry;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        File file = new File(Environment.getDefault().getConfig(), "auth");
        file = new File(file, "users.xml");
        registry = new SimpleUserRegistry(file);
        registry.loadRegistry(file);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        registry = null;
    }

}
