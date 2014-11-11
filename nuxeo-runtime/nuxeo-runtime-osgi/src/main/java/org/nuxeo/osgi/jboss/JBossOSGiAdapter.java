/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.osgi.jboss;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.management.Notification;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.SubDeployer;
import org.jboss.system.ListenerServiceMBeanSupport;
import org.jboss.system.server.Server;
import org.jboss.system.server.ServerConfig;
import org.jboss.system.server.ServerConfigLocator;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.osgi.SystemBundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;

/**
 * This service is a singleton (you must not deploy several times a service
 * using this implementation).
 * <p>
 * If you need to use this service inside multiple EARs you must use isolated
 * class loaders.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JBossOSGiAdapter extends ListenerServiceMBeanSupport implements JBossOSGiAdapterMBean {

    private static JBossOSGiAdapter instance;

    private OSGiAdapter osgi;

    /**
     * This method is safe only when using isolated EARs if there are multiple
     * nuxeo EARs in the system.
     *
     * @return the instance or null if not yet instantiated
     */
    public static JBossOSGiAdapter getInstance() {
        return instance;
    }

    /**
     * Gets the EAR deployment or null if no parent EAR was found.
     * <p>
     * This method should be used only on single Nuxeo deployments or when using
     * isolated class loaders for Nuxeo ears.
     *
     * @return
     */
    public static DeploymentInfo getEARDeployment() {
        if (instance == null) {
            throw new IllegalStateException("JBossOSGiAdapter was not initialized");
        }
        try {
            return instance.getDeploymentInfo().parent;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    public OSGiAdapter getOSGi() {
        return osgi;
    }

    @SuppressWarnings("unchecked")
    public BundleImpl installBundle(String symbolicName, DeploymentInfo di) throws BundleException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(di.ucl);
        try {
            log.info("Installing OSGi bundle: " + di.url);
            BundleImpl bundle;
            if ("org.nuxeo.osgi".equals(symbolicName)) {
                bundle = new SystemBundle(osgi, new JBossBundleFile(di),
                        di.ucl);
                osgi.setSystemBundle((SystemBundle)bundle);
                log.info("Installed system bundle: "+di.shortName);
            } else {
                bundle = new BundleImpl(osgi, new JBossBundleFile(di),
                    di.ucl);
            }
            di.context.put("OSGI_BUNDLE", bundle);
            osgi.install(bundle);
            return bundle;
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void uninstallBundle(String symbolicName, DeploymentInfo di) throws BundleException {
        BundleImpl bundle = (BundleImpl) di.context.remove("OSGI_BUNDLE");
        if (bundle != null) {
            log.info("Uninstalling OSGi bundle: " + di.url);
            osgi.uninstall(bundle);
        }
    }

    @Override
    protected void createService() throws Exception {
        super.createService();
        subscribe(true); // subscribe listeners
    }

    @Override
    protected void startService() throws Exception {
        super.startService();
        instance = this;
        ServerConfig jbossConfig = ServerConfigLocator.locate();
        File workingDir =  new File(jbossConfig.getServerDataDir(), "NXRuntime");
        File installDir = FileUtils.getFileFromURL(getEARDeployment().url);
        File configDir = new File(installDir, "config"); // TODO: must not use a directory from the ear.
        // initialize the Environment
        Environment env = new Environment(workingDir);
        env.setConfig(configDir);
        env.setLog( jbossConfig.getServerLogDir());
        env.setTemp(jbossConfig.getServerTempDir());
        env.setHostApplicationName(Environment.JBOSS_HOST);
        Package pkg = Package.getPackage("org.jboss");
        if (pkg == null) {
            env.setHostApplicationVersion("4.0.5.GA");
        } else {
            env.setHostApplicationVersion(pkg.getImplementationVersion());
        }
        env.setIsApplicationServer(true);
        Environment.setDefault(env);
        // start osgi adapter
        osgi = new OSGiAdapter(workingDir);
        osgi.setProperty("INSTALL_DIR", installDir.getAbsolutePath());
        osgi.setProperty("CONFIG_DIR", configDir.getAbsolutePath());
        String addr = System.getProperty("jboss.bind.address");
        if (addr != null) {
            osgi.setProperty("nuxeo.bind.address", addr);
        }
        //osgi.setProperty("HOST_ADAPTER", null); //TODO

//        DeploymentInfo di = getDeploymentInfo();
//        SystemBundle systemBundle = new SystemBundle(osgi, new JBossBundleFile(di),
//                di.ucl);
//        osgi.setSystemBundle(systemBundle);
    }

    @Override
    protected void stopService() throws Exception {
        super.stopService();
        osgi.shutdown();
        osgi = null;
    }

    @Override
    protected void destroyService() throws Exception {
        unsubscribe();
        super.destroyService();
    }

    public String listBundles() {
        BundleImpl[] bundles = osgi.getInstalledBundles();
        Arrays.sort(bundles, new Comparator<BundleImpl>() {
            public int compare(BundleImpl o1, BundleImpl o2) {
                return (int)(o1.getStartupTime() - o2.getStartupTime());
            }
        });
        StringBuilder buf = new StringBuilder();
        double total = 0;
        for (BundleImpl bundle : bundles) {
            buf.append(bundle.getBundleId()).append(": ")
                    .append(bundle.getSymbolicName()).append(" [ state: ")
                    .append(bundle.getState());
            double tm = bundle.getStartupTime();
            buf.append("; startup time: ").append(tm/1000);
            total += tm;
            buf.append(" ]\n");
        }
        buf.append("\n------------------------------------------------------------\nDeployed ")
            .append(bundles.length)
            .append("  bundles in ").append(total/1000).append(" sec.");
        return buf.toString();
    }

    public String getHomeLocation() {
        return osgi.getWorkingDir().getAbsolutePath();
    }

    /**
     * Handles notification.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void handleNotification2(Notification notification, Object handback) {
        String type = notification.getType().intern();
        // test for the the server started notification to send the event to the osgi framework
        if (type == Server.START_NOTIFICATION_TYPE) {
            osgi.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, osgi.getSystemBundle(), null));
            return;
        }

        // a deployment notification
        try {
            DeploymentInfo di = (DeploymentInfo) notification.getUserData();
            if (di == null || di.isXML || di.isScript
                    || di.url.sameFile(getDeploymentInfo().url)) {
                return;
            }
            //TODO: components from runtime bundle will be deployed twice..
            if (type == SubDeployer.CREATE_NOTIFICATION) {
                // check for OSGi bundles
                Manifest mf = di.getManifest();
                if (mf != null) {
                    Attributes mainAttributes = mf.getMainAttributes();
                    String val = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
                    if (val != null) {
                        di.context.put(Constants.BUNDLE_SYMBOLICNAME, val);
                        val = mainAttributes.getValue(Constants.BUNDLE_CLASSPATH);
                        String classPath = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
                        if (val != null) {
                            if (classPath != null) {
                                classPath += ',' + val;
                            } else {
                                classPath = val;
                            }
                            // update classpath so that the main deployer deploys these ones
                            mainAttributes.put(Attributes.Name.CLASS_PATH, classPath);
                        }
                    }
                }
            } else if (type == SubDeployer.START_NOTIFICATION) {
                String symbolicName = (String) di.context.get(Constants.BUNDLE_SYMBOLICNAME);
                if (symbolicName != null) {
                    installBundle(symbolicName, di);
                }
            } else if (type == SubDeployer.STOP_NOTIFICATION) {
                String symbolicName = (String) di.context.get(Constants.BUNDLE_SYMBOLICNAME);
                if (symbolicName != null) {
                    uninstallBundle(symbolicName, di);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("NXRuntime deployment failed", e);
        }
    }

}
