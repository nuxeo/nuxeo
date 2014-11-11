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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.runtime.jboss.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.EARDeployer;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.ServiceControllerMBean;
import org.nuxeo.common.collections.DependencyTree;
import org.nuxeo.common.logging.JavaUtilLoggingHelper;
import org.nuxeo.runtime.jboss.deployment.preprocessor.ContainerDescriptor;
import org.nuxeo.runtime.jboss.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.jboss.deployment.preprocessor.FragmentDescriptor;
import org.nuxeo.runtime.jboss.deployment.preprocessor.FragmentRegistry;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
@SuppressWarnings({"ResultOfObjectAllocationIgnored"})
public class NuxeoDeployer extends EARDeployer implements NuxeoDeployerMBean {

    public static final String LIB_DIR = "lib";
    public static final String CONFIG_DIR = "config";
    public static final String DS_DIR = "datasources";
    public static final String MBEANS_DIR = CONFIG_DIR + "/mbeans";

    /** The suffixes we accept, along with their relative order. */
    private static final String[] DEFAULT_ENHANCED_SUFFIXES = {
          "650:.ear", // from EARDeployer
          // additional extension -> but do not use them  because there are some inconsistencies
          // in jboss in how ejb3 mbean names (and jndi bean bindings) are generated.
          // For example when using other extensions than .ear WebServices are no more
          // working because they generate diferently mbean names than ejb3 deployer
          "850:.nxar",
          "850:.nxp",
          "850:.nux",
          "850:.ecm",
    };

    protected final ServiceControllerMBean controller;

    private DeploymentPreprocessor processor;
    private boolean debug = true;

    // workaround that fix subdeployment deps - which is not handled correctly by jboss 1.4.x
    private List<DeploymentInfo> subDeployments;


    /**
     * Default CTOR.
     */
    public NuxeoDeployer() {
        setEnhancedSuffixes(DEFAULT_ENHANCED_SUFFIXES);
        // controller = ServiceLocator.getServiceController();
        //we do not use ServiceLocator because of isolation issues
        controller = (ServiceControllerMBean) MBeanProxyExt.create(
                ServiceControllerMBean.class, ServiceControllerMBean.OBJECT_NAME,
                MBeanServerLocator.locateJBoss());
    }

    public void setDebug(boolean value) {
        debug = value;
    }

    public boolean isDebug() {
        return debug;
    }

    public void redeploy(String shortName) throws DeploymentException {
        shortName = '/' + shortName;
        for (DeploymentInfo deployment : subDeployments) {
            if (deployment.shortName.equals(shortName)) {
                mainDeployer.redeploy(deployment);
                return;
            }
        }
        throw new DeploymentException("Deployment not found: " + shortName);
    }

    public String listDeployments() {
        StringBuilder buf = new StringBuilder();
        for (DeploymentInfo deployment : subDeployments) {
            buf.append(deployment.shortName).append("\r\n");
        }
        return buf.toString();
    }

    public String[] getDeployments() {
        DeploymentInfo[] dinfos = subDeployments.toArray(new DeploymentInfo[subDeployments.size()]);
        String[] result = new String[dinfos.length];
        for (int i = 0; i < dinfos.length; i++) {
            result[i] = dinfos[i].shortName;
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean accepts(DeploymentInfo sdi) {
        if (super.accepts(sdi)) {
            if (sdi.isDirectory && hasContainerDescriptor(sdi) && isPreprocessingEnabled(sdi)) {
                sdi.context.put("EAR_PREPROCESSING", Boolean.TRUE);
            }
            return true;
        }
        // only directory are accepted
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(DeploymentInfo di) throws DeploymentException {
        JavaUtilLoggingHelper.redirectToApacheCommons();
        if (!canPreprocess(di)) {
            super.init(di);
            return;
        }
        try {
            String url = di.localUrl.toString();
            url = url.replace(" ", "%20");
            File directory = new File(new URI(url));
            loadSystemProperties(directory);
            processor = new DeploymentPreprocessor(directory);
            // initialize
            processor.init();
            // and predeploy
            processor.predeploy();

            FragmentRegistry freg = processor.getRootContainer().fragments;
            //freg.get("org.nuxeo.osgi");
            FragmentDescriptor fd = freg.get("org.nuxeo.osgi");
            if (fd != null) {
                di.addLibraryJar(new File(directory, fd.filePath).toURL());
            }
            //TODO this should not be hardcoded here -> find a fix for this
            di.addLibraryJar(new File(directory, "lib/osgi-core-4.1.jar").toURL());

            //di.addLibraryJar(new File(directory, "system/").toURL());
            // let the EAR deployer doing its job
            super.init(di);

            // handle special dirs
            Collection<String> firstDeployments = processStaticLibraries(di);
            Collection<String> mbeansDeployments = processNestedMBeans(di);
            firstDeployments.addAll(mbeansDeployments);
            Collection<String> dsDeployments = processNestedDataSources(di);
            firstDeployments.addAll(dsDeployments);
            Collection<String> lastDeployments = processConfig(di);

            // process any -bundle.xml
            // this way we are sure NXRuntime is initialized
            _processNestedDeployments(di);

            // ---------------- hack to fix dep order ------------------

            ContainerDescriptor root = processor.getRootContainer();
            if (root != null) {
                log.info("Applying sub-deployments ordering workaround");
                // copy sub-deployments
                subDeployments = new ArrayList<DeploymentInfo>(di.subDeployments);
                // clear subdeployments to avoid letting the jboss MainDeployer mess the ordering
                di.subDeployments.clear();
                //EARDeployer is breaking order because modules are split in two separate files:
                //appplication.xml and jboss-app.xml
                DeploymentSorter sorter = new DeploymentSorter(root);
                sorter.addFirst(firstDeployments);
                sorter.addLast(lastDeployments);
                Collections.sort(subDeployments, sorter);

                if (log.isInfoEnabled()) {
                    StringBuilder logBuf = new StringBuilder("Sub Deployment order is:\n");
                    for (DeploymentInfo sub : subDeployments) {
                        logBuf.append("     ").append(sub.shortName).append(
                                '\n');
                    }
                    logBuf.setLength(logBuf.length() - 1); // strip trailing LF
                    log.info(logBuf.toString());
                }
                List<DependencyTree.Entry<String, FragmentDescriptor>> pendingEntries = root.fragments.getPendingEntries();
                if (pendingEntries.isEmpty()) {
                    log.info("No unresolved Sub Deployments");
                } else {
                    StringBuilder msgs = new StringBuilder();
                    for (DependencyTree.Entry<String, FragmentDescriptor> entry : pendingEntries) {
                        String msg = "Unresolved Sub Deployment: " +
                                entry.getKey() + ": " + entry.getWaitsFor();
                        log.error(msg);
                        msgs.append(msg).append('\n');
                    }
                    // store errors in a system property for OSGiRuntimeService
                    // to retrieve
                    System.setProperty("org.nuxeo.runtime.deployment.errors",
                            msgs.toString());
                }
            }

            // ------------------ hack end -----------------------

        } catch (URISyntaxException e) {
            throw new DeploymentException("Failed to get deployment directory for " + di.shortName, e);
        } catch (Exception e) {
            throw new DeploymentException("Deployment preprocessing failed for " + di.shortName, e);
        }
    }

    @Override
    public void start(DeploymentInfo di) throws DeploymentException {
        if (!canPreprocess(di)) {
            super.start(di);
            return;
        }
        // ---------------- hack to fix dep order ------------------
        if (subDeployments != null) {
            for (DeploymentInfo sub : subDeployments) {
                mainDeployer.deploy(sub);
            }
        }
        // ------------------ hack end -----------------------

        super.start(di);

        writeStatusFile(di);
    }

    @Override
    public void stop(DeploymentInfo di) throws DeploymentException {
        if (!canPreprocess(di)) {
            super.stop(di);
            return;
        }

        super.stop(di);

        // ---------------- hack to fix dep order ------------------
        if (subDeployments != null) {
            for (DeploymentInfo sub : subDeployments) {
                mainDeployer.undeploy(sub);
            }
        }
        // ------------------ hack end -----------------------
    }

    @Override
    public void destroy(DeploymentInfo di) throws DeploymentException {
        super.destroy(di);
        JavaUtilLoggingHelper.reset();
    }

    /**
     * Data sources must be deployed first.
     *
     * @param di
     * @throws DeploymentException
     */
    protected Collection<String> processNestedDataSources(DeploymentInfo di)
            throws DeploymentException {
        List<String>names = new ArrayList<String>();
        File directory = new File(getEarDirectory(di), DS_DIR);
        if (!directory.isDirectory()) {
            return names;
        }
        for (String fileName : directory.list()) {
            try {
                if (fileName.endsWith("-ds.xml")) {
                    log.info("Found DataSource subdeployment: " + fileName);
                    new DeploymentInfo(new URL(di.url, DS_DIR + '/' + fileName), di, getServer());
                    names.add(fileName);
                }
            } catch (Exception e) {
                log.error("Failed to create subdeployment for " + fileName, e);
            }
        }
        return names;
    }

    /**
     * Deploy the JBoss mbeans used to confiugre specific JBoss
     * services like topics, etc.
     * <p>
     * MBeans are deployed before any other bundle.
     *
     * @param di
     * @return
     * @throws DeploymentException
     */
    protected Collection<String> processNestedMBeans(DeploymentInfo di) throws DeploymentException {
        List<String>names = new ArrayList<String>();
        File directory = new File(getEarDirectory(di), MBEANS_DIR);
        if (!directory.isDirectory()) {
            return names;
        }
        for (String fileName : directory.list()) {
            try {
                if (fileName.endsWith("-service.xml")) {
                    log.info("Found DataSource subdeployment: " + fileName);
                    new DeploymentInfo(new URL(di.url, MBEANS_DIR + '/' + fileName), di, getServer());
                    names.add(fileName);
                }
            } catch (Exception e) {
                log.error("Failed to create subdeployment for " + fileName, e);
            }
        }
        return names;
    }

    protected Collection<String> processStaticLibraries(DeploymentInfo di)
            throws DeploymentException {
        List<String>names = new ArrayList<String>();
        File directory = new File(getEarDirectory(di), LIB_DIR);
        if (!directory.isDirectory()) {
            return names;
        }
        for (String fileName : directory.list()) {
            try {
                if (fileName.endsWith(".jar")) {
                    log.info("Found library: " + fileName);
                    new DeploymentInfo(new URL(di.url, LIB_DIR + '/' + fileName), di, getServer());
                    names.add(fileName);
                }
            } catch (Exception e) {
                log.error("Failed to create subdeployment for " + fileName, e);
            }
        }
        return names;
    }

    protected Collection<String> processConfig(DeploymentInfo di) throws DeploymentException {
        List<String>names = new ArrayList<String>();
        File directory = new File(getEarDirectory(di), CONFIG_DIR);
        if (!directory.isDirectory()) {
            return names;
        }
        for (String fileName : directory.list()) {
            try {
                if (fileName.endsWith("-config.xml") || fileName.endsWith("-bundle.xml")) {
                    log.info("Found deployable configuration: " + fileName);
                    new DeploymentInfo(new URL(di.url, CONFIG_DIR + '/' + fileName), di, getServer());
                    names.add(fileName);
                }
            } catch (Exception e) {
                log.error("Failed to create subdeployment for " + fileName, e);
            }
        }
        return names;
    }

    // wokraround to fix -bundle.xml deployment
    protected void _processNestedDeployments(DeploymentInfo di) throws DeploymentException {
        // deploy "-bundle.xml" files
        File directory = getEarDirectory(di);
        for (String fileName : directory.list()) {
            try {
                if (fileName.endsWith("-bundle.xml")) {
                    log.info("Found XML bundle subdeployment: " + fileName);
                    new DeploymentInfo(new URL(di.url, fileName), di, getServer());
                }
            } catch (Exception e) {
                log.error("Failed to create subdeployment for " + fileName, e);
            }
        }
    }

    class DeploymentSorter implements Comparator<DeploymentInfo>, Serializable {

        private static final long serialVersionUID = -1221994800538028156L;

        private final Map<String, Integer> map = new HashMap<String, Integer>();

        DeploymentSorter(ContainerDescriptor container) {
            int i = 0;
            for (DependencyTree.Entry<String, FragmentDescriptor> entry
                    : container.fragments.getResolvedEntries()) {
                FragmentDescriptor fd = entry.get();
                map.put(fd.name, i++);
            }
            // deploy the system.bundle first - TODO parametrize this
            map.put("org.nuxeo.osgi", -1);
        }

        public void addFirst(Collection<String> deploymentNames) {
            for (String name : deploymentNames) {
                map.put(name, -1);
            }
        }

        public void addLast(Collection<String> deploymentNames) {
            for (String name : deploymentNames) {
                map.put(name, Integer.MAX_VALUE);
            }
        }

        public void addFirst(String deploymentName) {
            map.put(deploymentName, -1);
        }

        public void addLast(String deploymentName) {
            map.put(deploymentName, Integer.MAX_VALUE);
        }

        public int compare(DeploymentInfo o1, DeploymentInfo o2) {

            String name1 = processor.getJarId(o1.shortName);
            if (name1 == null) {
                name1 = o1.shortName;
            }
            String name2 = processor.getJarId(o2.shortName);
            if (name2 == null) {
                name2 = o2.shortName;
            }

            Integer k1 = map.get(name1);
            Integer k2 = map.get(name2);

            if (k1 == null) {
                k1 = -1;
            }
            if (k2 == null) {
                k2 = -1;
            }
            return k1 - k2;
        }

    }

    public static boolean canPreprocess(DeploymentInfo di) {
        return di.context.containsKey("EAR_PREPROCESSING");
    }

    public static boolean hasContainerDescriptor(DeploymentInfo di)  {
        return di.localCl.findResource("OSGI-INF/deployment-container.xml") != null;
    }

    public final boolean isPreprocessingEnabled(DeploymentInfo di) {
        try {
            return debug || isFirstRun(di);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected static File getEarDirectory(DeploymentInfo di) throws DeploymentException {
        try {
            String url = di.localUrl.toString();
            url = url.replace(" ", "%20");
            return new File(new URI(url));
        } catch (Exception e) {
            throw new DeploymentException("Cannot get deploying directory: " + di.shortName, e);
        }
    }

    protected static File getPredeployStatusFile(DeploymentInfo di) throws DeploymentException {
        return new File(getEarDirectory(di), ".predeploy");
    }

    protected static boolean isFirstRun(DeploymentInfo di) throws DeploymentException {
        return !getPredeployStatusFile(di).exists();
    }

    protected static void writeStatusFile(DeploymentInfo di) throws DeploymentException {
        File file = getPredeployStatusFile(di);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                throw new DeploymentException("Cannot create predeployment status file for: "
                        + di.shortName, e);
            }
        }
    }

    public void loadSystemProperties(File dir) {
        File file = new File(dir, "config/system.properties");
        if (file.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                System.getProperties().load(in);
            } catch (Throwable t) {
                log.warn("Failed to load system properties", t);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

    }

}
