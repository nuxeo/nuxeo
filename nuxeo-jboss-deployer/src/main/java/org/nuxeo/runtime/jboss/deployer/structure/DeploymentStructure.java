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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.virtual.VirtualFile;
import org.nuxeo.runtime.jboss.deployer.Utils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DeploymentStructure {

    protected Map<String, String> properties = new HashMap<String, String>();

    /**
     * The resolved bundle relative paths - ordered by file path
     */
    protected String[] resolvedBundlePaths;

    protected final VirtualFile vhome;

    protected File home;

    protected String[] bundles;

    protected String[] children;

    protected final List<Context> ctxs = new ArrayList<Context>();

    protected boolean requirePreprocessing = true;

    protected String[] preprocessorClassPath;

    public DeploymentStructure(VirtualFile vhome) {
        this.vhome = vhome;
    }

    /**
     * Must be called immediately after the deployment structure was created
     * (before using the object).
     * <p>
     * The lastModified is optional - if you don't need it you must specify a
     * value of 0.
     */
    public void initialize(long lastModified) throws Exception {
        this.home = Utils.getRealHomeDir(vhome, lastModified).getCanonicalFile();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void setProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public String getProperty(String key, String defValue) {
        String v = properties.get(key);
        return v == null ? defValue : v;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Get the property value given a key. Any variable present in the value
     * will be expanded against the system properties.
     */
    public String expandProperty(String key, String defaultValue) {
        String v = properties.get(key);
        if (v != null) {
            return Utils.expandVars(v, System.getProperties());
        } else {
            return defaultValue;
        }
    }

    public void addContext(Context ctx) {
        ctxs.add(ctx);
    }

    public List<Context> getContexts() {
        return ctxs;
    }

    protected String[] resolveBundles() throws IOException {
        String[] bp = getBundles();
        if (bp == null || bp.length == 0) {
            return null;
        }
        PathMatcher matcher = new PathMatcher();
        matcher.addPatterns(bp);
        List<String> list = matcher.getMatchesAsPaths(getHome());
        resolvedBundlePaths = list.toArray(new String[list.size()]);
        Arrays.sort(resolvedBundlePaths);
        return resolvedBundlePaths;
    }

    public String[] getResolvedBundles() throws IOException {
        if (resolvedBundlePaths == null) {
            resolveBundles();
        }
        return resolvedBundlePaths;
    }

    public File[] getResolvedBundleFiles() throws IOException {
        if (getResolvedBundles() == null) {
            throw new IllegalStateException(
                    "You must call this method only after bundles are resolved");
        }
        File[] files = new File[resolvedBundlePaths.length];
        for (int i = 0; i < resolvedBundlePaths.length; i++) {
            files[i] = new File(home, resolvedBundlePaths[i]);
        }
        return files;
    }

    public File getHome() {
        if (home == null) {
            throw new IllegalStateException(
                    "The initialize method must be called before using the deployment structure object.");
        }
        return home;
    }

    public VirtualFile getVirtualHome() {
        return vhome;
    }

    public void setBundles(String... bundles) {
        this.bundles = bundles;
    }

    public String[] getBundles() {
        return bundles;
    }

    public void setRequirePreprocessing(boolean requirePreprocessing) {
        this.requirePreprocessing = requirePreprocessing;
    }

    public void setPreprocessorClassPath(String[] preprocessorClassPath) {
        this.preprocessorClassPath = preprocessorClassPath;
    }

    public String[] getPreprocessorClassPath() {
        return preprocessorClassPath;
    }

    public boolean isRequirePreprocessing() {
        return requirePreprocessing;
    }

    public void setChildren(String... children) {
        this.children = children;
    }

    public String[] getChildren() {
        return children;
    }

    public static class Context {
        protected final String path;

        protected String[] classpath;

        protected String[] metaDataPath;

        public Context(String path) {
            this.path = path == null ? "" : path;
        }

        public String getPath() {
            return path;
        }

        public void setClasspath(String... classpath) {
            this.classpath = classpath;
        }

        public String[] getClasspath() {
            return classpath;
        }

        public void setMetaDataPath(String... metaDataPath) {
            this.metaDataPath = metaDataPath;
        }

        public String[] getMetaDataPath() {
            return metaDataPath;
        }

    }

}
