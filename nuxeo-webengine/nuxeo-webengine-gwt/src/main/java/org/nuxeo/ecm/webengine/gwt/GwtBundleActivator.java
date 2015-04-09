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
 */
package org.nuxeo.ecm.webengine.gwt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * This activator must be used as an activator by bundles that wants to deploy GWT resources in a nuxeo server.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class GwtBundleActivator implements BundleActivator, FrameworkListener {

    protected static final Log log = LogFactory.getLog(GwtBundleActivator.class);

    public static final String GWT_DEV_MODE_PROP = "nuxeo.gwt_dev_mode";

    public static final boolean GWT_DEV_MODE = "true".equals(Framework.getProperty(GWT_DEV_MODE_PROP, "false"));

    protected BundleContext context;

    @Override
    public void start(BundleContext context) {
        this.context = context;
        if (GWT_DEV_MODE) {
            return;
        }
        context.addFrameworkListener(this);
    }

    @Override
    public void stop(BundleContext context) {
        this.context = null;
    }

    protected void installGwtApp(Bundle bundle) throws IOException, URISyntaxException {
        URL location = bundle.getEntry("gwt-war");
        if (location == null) {
            throw new IOException("Cannot locate gwt-war in " + bundle.getSymbolicName());
        }
        Framework.getService(GwtResolver.class).install(location.toURI());
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            try {
                installGwtApp(context.getBundle());
            } catch (IOException | URISyntaxException cause) {
                throw new RuntimeException("Cannot start GWT", cause);
            }
        }
    }

}
