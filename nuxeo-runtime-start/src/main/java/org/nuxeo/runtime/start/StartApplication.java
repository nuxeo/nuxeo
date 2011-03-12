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
package org.nuxeo.runtime.start;

import java.io.File;

import javax.naming.NamingException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * This bundle should be put in a startlevel superior than the one used to start nuxeo bundles.
 * When the bundle is started it will send an application started notification.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StartApplication implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
       configureLogging();
        initEnvironment();
        removeH2Lock();
        startWebServices(context);
        startRuntime(context);
        startContainer();
        ((OSGiRuntimeService)Framework.getRuntime()).fireApplicationStarted();
    }

    protected void removeH2Lock() {
        String h2 = System.getProperty("h2.baseDir");
        if (h2 != null) {
            File file = new File(h2);
            file = new File(file, "nuxeo.lucene");
            file = new File(file, "write.lock");
            file.delete();
        }
    }

    protected void startRuntime(BundleContext context) throws BundleException {
        try {
            context.getBundle().loadClass("org.nuxeo.runtime.api.Framework");
        } catch (Throwable t) {
            throw new BundleException("Cannot load framework", t);
        }
        // not in osgi.core r4
        //FrameworkUtil.getBundle(Framework.class).start();
    }

    protected void startContainer() throws NamingException {
        NamingContextFactory.install();
        NuxeoContainer.install();
    }

    protected void configureLogging() throws BundleException {
        if (System.getProperty("logback.configurationFile") != null) {
            return;
        }
        String home = System.getProperty("nuxeo.home");
        if (home == null) {
            return;
        }
        final String config = home.concat("/config/logback.xml");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        try {
            configurator.doConfigure(config);
        } catch (JoranException e) {
            throw new BundleException("Cannot configure logging from " + config, e);
        }
    }

    protected void startWebServices(BundleContext context)
            throws BundleException {
        ServiceReference pkgAdmin = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = (PackageAdmin)context.getService(pkgAdmin);
        tryStartJetty(pa);
        context.ungetService(pkgAdmin);
    }

    protected boolean tryStartJetty(PackageAdmin pa) {
        Bundle[] bundles = pa.getBundles("org.eclipse.equinox.http.jetty", null);
        if (bundles != null && bundles.length > 0) {
            try {
                bundles[0].start();
                return true;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Initialize environment using default values if no specified on the command line
     */
    protected void initEnvironment() {
        String home = System.getProperty("nuxeo.home");
        if (home == null) {
            home = System.getProperty("user.home")+File.separator+".nxserver-osgi";
            System.setProperty("nuxeo.home", home);
        }
        String v = System.getProperty("h2.baseDir");
        if (v == null) {
            System.setProperty("h2.baseDir", home+File.separator+"data"+File.separator+"h2");
        }
        v = System.getProperty("jetty.logs");
        if (v == null) {
            System.setProperty("jetty.logs", home+File.separator+"log");
        }
        v = System.getProperty("org.eclipse.equinox.http.jetty.http.port");
        if (v == null) {
            System.setProperty("org.eclipse.equinox.http.jetty.http.port", "8080");
        }
        v = System.getProperty("logback.configurationFile");
        if (v == null) {
            System.setProperty("logback.configurationFile", home+File.separator+"config"+File.separator+"logback.xml");
        }
    }

    public void stop(BundleContext context) throws Exception {

    }
}
