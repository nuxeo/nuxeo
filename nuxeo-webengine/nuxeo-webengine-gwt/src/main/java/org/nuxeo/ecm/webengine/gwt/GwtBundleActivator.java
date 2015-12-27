/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.gwt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
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

    public static final File GWT_ROOT = new File(Environment.getDefault().getWeb(), "root.war/gwt");

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
