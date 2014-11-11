/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.osgi.jboss;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.jboss.deployment.DeploymentInfo;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleManifestReader;
import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JBossBundleFile implements BundleFile {

    private final DeploymentInfo di;

    public JBossBundleFile(DeploymentInfo di) {
        this.di = di;
    }

    protected DeploymentInfo getDeploymentInfo() {
        return di;
    }

    @Override
    public Enumeration<URL> findEntries(String path, String pattern,
            boolean recurse) {
        throw new UnsupportedOperationException("The operation Bundle.findEntries() was not yet implemented");
    }

    public ClassLoader getClassLoader() {
        return di.ucl;
    }

    public void setClassLoader(ClassLoader classLoader) {
        throw new UnsupportedOperationException("Cannot set the class loade rof a JBoss bundle deployment");
    }

    @Override
    public URL getEntry(String name) {
        return di.localCl.findResource(name); //TODO
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        throw new UnsupportedOperationException("The operation Bundle.geEntryPaths() was not yet implemented");
    }

    @Override
    public String getLocation() {
        return di.url.toExternalForm();
    }

    @Override
    public Manifest getManifest() {
        return di.getManifest();
    }

    @Override
    public URL getURL() {
        return di.url;
    }

    @Override
    public File getFile() {
        return FileUtils.getFileFromURL(di.url);
    }

    @Override
    public String getFileName() {
        return new File(di.url.getFile()).getName();
    }

    @Override
    public String getSymbolicName() {
        String value = di.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        return value != null ? BundleManifestReader.removePropertiesFromHeaderValue(value) : null;
    }

    @Override
    public Collection<BundleFile> getNestedBundles(File tmpDir)
            throws IOException {
        return null;
    }

    @Override
    public Collection<BundleFile> findNestedBundles(File tmpDir)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDirectory() {
        return di.isDirectory;
    }

    @Override
    public boolean isJar() {
        return !di.isDirectory && !di.isXML && !di.isScript;
    }

    @Override
    public void close(OSGiAdapter osgi) throws IOException {
        return;
    }

}
