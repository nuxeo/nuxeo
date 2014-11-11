/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public URL getEntry(String name) {
        return di.localCl.findResource(name); //TODO
    }

    public Enumeration<String> getEntryPaths(String path) {
        throw new UnsupportedOperationException("The operation Bundle.geEntryPaths() was not yet implemented");
    }

    public String getLocation() {
        return di.url.toExternalForm();
    }

    public Manifest getManifest() {
        return di.getManifest();
    }

    public URL getURL() {
        return di.url;
    }

    public File getFile() {
        return FileUtils.getFileFromURL(di.url);
    }

    public String getFileName() {
        return new File(di.url.getFile()).getName();
    }

    public String getSymbolicName() {
        String value = di.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        return value != null ? BundleManifestReader.removePropertiesFromHeaderValue(value) : null;
    }

    public Collection<BundleFile> getNestedBundles(File tmpDir)
            throws IOException {
        return null;
    }

    public Collection<BundleFile> findNestedBundles(File tmpDir)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isDirectory() {
        return di.isDirectory;
    }

    public boolean isJar() {
        return !di.isDirectory && !di.isXML && !di.isScript;
    }

}
