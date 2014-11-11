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
package org.nuxeo.runtime.jboss.deployer.structure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.structure.ContextInfo;
import org.jboss.deployers.spi.structure.StructureMetaDataFactory;
import org.jboss.deployers.vfs.plugins.structure.AbstractVFSStructureDeployer;
import org.jboss.deployers.vfs.spi.structure.StructureContext;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.ModulesMetaData;
import org.jboss.virtual.VirtualFile;
import org.jboss.xb.binding.Unmarshaller;
import org.jboss.xb.binding.UnmarshallerFactory;
import org.jboss.xb.binding.sunday.unmarshalling.SchemaBindingResolver;
import org.jboss.xb.binding.sunday.unmarshalling.SingletonSchemaResolverFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoStructureDeployer extends AbstractVFSStructureDeployer {

    public static final String NUXEO_STRUCTURE_XML = "META-INF/nuxeo-structure.xml";

    public static final String NUXEO_PREPROCESSOR_FILE = "META-INF/nuxeo-preprocessor.xml";

    /**
     * I don't know how to propagate information between structure deployers and
     * deployers I tried context getPredeterminedManagedObjects() but without
     * success. So I am using a static field to propagate the
     * DeploymentStructure.
     *
     * This is useful to avoid parsing and resolving bundles twice.
     */
    protected static Map<String, DeploymentStructure> structures;

    public static synchronized DeploymentStructure popStructure(String key) {
        if (structures != null) {
            DeploymentStructure ds = structures.remove(key);
            if (structures.isEmpty()) {
                structures = null;
            }
            return ds;
        }
        return null;
    }

    public static synchronized DeploymentStructure peekStructure(String key) {
        if (structures != null) {
            return structures.get(key);
        }
        return null;
    }

    public static synchronized void pushStructure(String key,
            DeploymentStructure ds) {
        if (structures == null) {
            structures = new HashMap<String, DeploymentStructure>();
        }
        structures.put(key, ds);
    }

    /** whether to validate deployment descriptors */
    protected boolean useValidation = true;

    protected boolean includeEarRootInClasspath = true;

    protected boolean includeManifestClassPath = false;

    /**
     * The schema resolver used to determine which schema to use for
     * application.xml/jboss-app.xml
     */
    protected SchemaBindingResolver resolver = SingletonSchemaResolverFactory.getInstance().getSchemaBindingResolver();

    /** unmarshaller factory */
    protected UnmarshallerFactory unmarshallerFactory = UnmarshallerFactory.newInstance();

    public NuxeoStructureDeployer() {
        setRelativeOrder(10); // put it in front of regular deployers.
    }

    /**
     * Gets the schema resolver.
     *
     * @return the schema resolver
     */
    public SchemaBindingResolver getResolver() {
        return resolver;
    }

    /**
     * Sets the schema resolver.
     *
     * @param resolver the schema resolver
     */
    public void setResolver(SchemaBindingResolver resolver) {
        this.resolver = resolver;
    }

    public void setIncludeEarRootInClasspath(boolean includeEarRootInClasspath) {
        this.includeEarRootInClasspath = includeEarRootInClasspath;
    }

    public boolean isIncludeEarRootInClasspath() {
        return includeEarRootInClasspath;
    }

    public void setIncludeManifestClassPath(boolean includeManifestClassPath) {
        this.includeManifestClassPath = includeManifestClassPath;
    }

    public boolean isIncludeManifestClassPath() {
        return includeManifestClassPath;
    }

    public void setUseValidation(boolean validateXml) {
        this.useValidation = validateXml;
    }

    public boolean isUseValidation() {
        return useValidation;
    }

    @Override
    public boolean determineStructure(StructureContext structureContext)
            throws DeploymentException {
        VirtualFile root = structureContext.getFile();
        try {
            double start = System.currentTimeMillis();
            DeploymentStructure md = accept(root);
            if (md == null) { // not a nuxeo deployment
                return false;
            }
            md.initialize(0);
            log.info("Found Nuxeo Deployment: " + root.getName());
            File[] bundles = md.getResolvedBundleFiles();
            // launch preprocessor if needed
            if (md.isRequirePreprocessing()) {
                preprocess(md, bundles);
            }
            // if an ear load the metadata and create ear subdeployments
            JBossAppMetaData earMetaData = loadEarMetaData(root);
            if (earMetaData != null) {
                createEarSubdeployments(structureContext, earMetaData);
            }

            // create deployments specified in structure meta data
            for (DeploymentStructure.Context ctx : md.getContexts()) {
                createContext(structureContext, ctx);
            }

            // create children deployments if any
            String[] children = md.getChildren();
            if (children != null) {
                for (String c : children) {
                    VirtualFile child = root.getChild(c);
                    if (child != null) {
                        structureContext.determineChildStructure(child);
                    } else {
                        log.warn("Sub-deployment " + c + " not found in "
                                + root.getName());
                    }
                }
            }
            // compute the bundles classpath and set it to the root context and
            createBundlesClasspath(structureContext, md, earMetaData);
            // push the computed bundles to the static stack to be available for
            // nuxeo launcher.
            pushStructure(root.getName(), md);
            log.info("Determining Nuxeo Deployment Structure took "
                    + ((System.currentTimeMillis() - start) / 1000) + " sec.");
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
        return true;

    }

    protected void createContext(StructureContext structureContext,
            DeploymentStructure.Context ctx) throws IOException {
        VirtualFile root = structureContext.getFile();
        String path = ctx.getPath();
        if (path.indexOf('*') > -1) {
            PathPattern pattern = PathPattern.parse(path);
            path = pattern.findFirstMatchingPath(root);
            if (path == null) {
                log.warn("No such context was found " + ctx.getPath() + " in "
                        + root.getName());
                return;
            }
        }
        ContextInfo ci = createNamedContext(structureContext, path);
        String[] mp = ctx.getMetaDataPath();
        if (mp != null) {
            for (String p : mp) {
                ci.addMetaDataPath(p);
            }
        }
        String[] cp = ctx.getClasspath();
        if (cp != null) {
            PathMatcher matcher = new PathMatcher();
            matcher.addPatterns(cp);
            List<VirtualFile> cpFiles = matcher.getMatches(root);
            for (VirtualFile file : cpFiles) {
                addClassPath(structureContext, file, true,
                        includeManifestClassPath, ci);
            }
        }
    }

    protected ContextInfo createNamedContext(StructureContext structureContext,
            String name) {
        // Create and link the context
        ContextInfo context = StructureMetaDataFactory.createContextInfo(name,
                null);
        applyStructure(structureContext, context);
        return context;
    }

    public static DeploymentStructure loadConfig(VirtualFile root)
            throws Exception {
        VirtualFile vf = root.getChild(NUXEO_STRUCTURE_XML);
        if (vf == null) {
            return null;
        }
        InputStream in = vf.openStream();
        try {
            return new DeploymentStructureReader().read(root, in);
        } finally {
            in.close();
        }
    }

    public static DeploymentStructure accept(VirtualFile root) throws Exception {
        if (root.isLeaf()) {
            return null;
        }
        return loadConfig(root);
    }

    /**
     * We should avoid putting JARs twice in the classpath: the EAR modules will
     * be put in the classpath by the children deployments. so we need to remove
     * EAR modules from he bundles list we put in the root context classpath to
     * avoid having EJB2 deployed twice. (one for the EAR as EAr module and once
     * as global EJBs for the jar module).
     */
    protected void createBundlesClasspath(StructureContext structureContext,
            DeploymentStructure md, JBossAppMetaData earMd) throws Exception {
        ContextInfo ci = structureContext.getMetaData().getContext("");
        if (ci == null) {
            throw new DeploymentException(
                    "A Nuxeo application must declare a root context (a context with path == \"\")");
        }

        VirtualFile root = structureContext.getFile();
        String[] paths = md.getResolvedBundles();
        if (paths == null || paths.length == 0) {
            throw new DeploymentException(
                    "A Nuxeo application must declare at last one bundle");
        }

        addClassPath(structureContext, root, includeEarRootInClasspath,
                includeManifestClassPath, ci);

        ModulesMetaData modules = null;
        if (earMd != null) {
            modules = earMd.getModules();
        }

        if (modules != null) {
            applyClassPath(structureContext, ci, md.getHome(), root, paths,
                    modules);
        } else {
            applyClassPath(structureContext, ci, md.getHome(), root, paths);
        }
    }

    protected void applyClassPath(StructureContext structureContext,
            ContextInfo ci, File home, VirtualFile root, String[] paths,
            ModulesMetaData modules) throws IOException {
        for (String path : paths) {
            // check first if the bundle is an EAR module
            if (modules.get(path) == null) {
                VirtualFile vfile = root.getChild(path);
                if (vfile == null) {
                    throw new IOException("Relative file " + path
                            + "doesn't exists");
                }
                addClassPath(structureContext, vfile, true,
                        includeManifestClassPath, ci);
            } else {
                // the cp entry will be added by the EAR module deployment.
                log.debug("Not adding EAR module to root classpath: " + path);
            }
        }
    }

    protected void applyClassPath(StructureContext structureContext,
            ContextInfo ci, File home, VirtualFile root, String[] paths)
            throws IOException {
        for (String path : paths) {
            VirtualFile vfile = root.getChild(path);
            if (vfile == null) {
                throw new IOException("Relative file " + path
                        + "doesn't exists");
            }
            addClassPath(structureContext, vfile, true,
                    includeManifestClassPath, ci);
        }
    }

    protected ClassLoader createPreprocessorClassLoader(DeploymentStructure md)
            throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = NuxeoStructureDeployer.class.getClassLoader();
        }
        String[] cp = md.getPreprocessorClassPath();
        if (cp != null && cp.length > 0) {
            PathMatcher matcher = new PathMatcher();
            matcher.addPatterns(cp);
            List<File> cpFiles = matcher.getMatchesAsFiles(md.getHome());
            URL[] urls = new URL[cpFiles.size()];
            for (int i = 0; i < urls.length; i++) {
                urls[i] = cpFiles.get(i).toURI().toURL();
            }
            cl = new URLClassLoader(urls, cl);
        }
        return cl;
    }

    public void preprocess(DeploymentStructure md, File[] bundles)
            throws Exception {
        double start = System.currentTimeMillis();
        // XXX remove this property when deployment-fragment.xml will be fixed
        System.setProperty("org.nuxeo.runtme.preprocessing.jboss5", "true");
        File metadata = new File(md.getHome(), NUXEO_PREPROCESSOR_FILE);
        if (!metadata.isFile()) {
            log.error("Unable to find " + NUXEO_PREPROCESSOR_FILE
                    + " file. Skip preprocessing.");
            return;
        }
        // create the classloader to load the preprocessor
        ClassLoader cl = createPreprocessorClassLoader(md);
        Class<?> klass = cl.loadClass("org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor");
        Method process = klass.getMethod("process", File.class, File.class,
                File[].class);
        process.invoke(null, md.getHome(), metadata, bundles);
        log.info("Nuxeo Preprocessing took "
                + ((System.currentTimeMillis() - start) / 1000) + " sec.");
    }

    /** The EAR processing stuff */

    public JBossAppMetaData loadEarMetaData(VirtualFile file) throws Exception {

        VirtualFile applicationXml = file.getChild("META-INF/application.xml");
        VirtualFile jbossAppXml = file.getChild("META-INF/jboss-app.xml");
        if (applicationXml == null && jbossAppXml == null) {
            return null; // not an ear
        }

        Unmarshaller unmarshaller = unmarshallerFactory.newUnmarshaller();
        unmarshaller.setValidation(useValidation);
        EarMetaData specMetaData = null;
        if (applicationXml != null) {
            InputStream in = applicationXml.openStream();
            try {
                specMetaData = (EarMetaData) unmarshaller.unmarshal(in,
                        resolver);
            } finally {
                in.close();
            }
        }
        JBossAppMetaData appMetaData = null;
        if (jbossAppXml != null) {
            InputStream in = jbossAppXml.openStream();
            try {
                appMetaData = (JBossAppMetaData) unmarshaller.unmarshal(in,
                        resolver);
            } finally {
                in.close();
            }
        }
        // Need a metadata instance and there will not be one if there are no
        // descriptors
        if (appMetaData == null) {
            appMetaData = new JBossAppMetaData();
        }
        // Create the merged view
        appMetaData.merge(appMetaData, specMetaData);
        return appMetaData;
    }

    protected void createEarSubdeployments(StructureContext structureContext,
            JBossAppMetaData appMetaData) throws Exception {
        VirtualFile file = structureContext.getFile();
        // Create subdeployments for the ear modules
        ModulesMetaData modules = appMetaData.getModules();
        if (modules != null) {
            for (ModuleMetaData mod : modules) {
                String fileName = mod.getFileName();
                if (fileName != null
                        && (fileName = fileName.trim()).length() > 0) {
                    if (log.isTraceEnabled()) {
                        log.trace("Checking application.xml module: "
                                + fileName);
                    }

                    try {
                        VirtualFile module = file.getChild(fileName);
                        if (module == null) {
                            throw new RuntimeException(
                                    fileName
                                            + " module listed in application.xml does not exist within .ear "
                                            + file.toURI());
                        }
                        // Ask the deployers to analyze this
                        if (!structureContext.determineChildStructure(module)) {
                            throw new RuntimeException(
                                    fileName
                                            + " module listed in application.xml is not a recognized deployment, .ear: "
                                            + file.getName());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Exception looking for "
                                + fileName
                                + " module listed in application.xml, .ear "
                                + file.getName(), e);
                    }
                }
            }
        }
    }

}
