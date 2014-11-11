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
