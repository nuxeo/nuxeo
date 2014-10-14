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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This activator must be used as an activator by bundles that wants to deploy
 * GWT resources in a nuxeo server.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class GwtBundleActivator implements BundleActivator {

    protected static final Log log = LogFactory.getLog(GwtBundleActivator.class);

    public static final String GWT_DEV_MODE_PROP = "nuxeo.gwt_dev_mode";

    public static final File GWT_ROOT = new File(
            Environment.getDefault().getWeb(), "root.war/gwt");

    public static final boolean GWT_DEV_MODE = "true".equals(System.getProperty(
            GWT_DEV_MODE_PROP, "false"));

    protected BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        if (GWT_DEV_MODE) {
            return;
        }
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
               if (event.id != RuntimeServiceEvent.RUNTIME_STARTED) {
                   return;
               }
               Framework.removeListener(this);
               try {
                   installGwtApp(GwtBundleActivator.this.context.getBundle());
               } catch (Exception cause) {
                   log.error("Cannot install gwt app", cause);
               }
            }
        });
    }


    @Override
    public void stop(BundleContext context) throws Exception {
        this.context = null;
    }

    protected void installGwtApp(Bundle bundle) throws Exception {
        GWT_ROOT.mkdirs();
        String symName = bundle.getSymbolicName();
        // check the marker file to avoid copying twice
        File markerFile = new File(GWT_ROOT, ".metadata/" + symName);
        File file = Framework.getRuntime().getBundleFile(bundle);
        if (file == null) {
            log.warn("A GWT module without a war directory inside");
            return;
        }
        if (markerFile.lastModified() < file.lastModified()) {
            log.info("Installing GWT Application from bundle " + symName);
            ZipUtils.unzip("gwt-war", file, GWT_ROOT);
            markerFile.getParentFile().mkdirs();
            markerFile.createNewFile();
        }
    }



}
