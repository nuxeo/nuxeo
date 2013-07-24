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

package org.nuxeo.osgi;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class OSGiBundle implements Bundle {

    protected long id = -1;

    protected OSGiSystemContext osgi;

    protected final String symbolicName;

    protected final Version version;

    protected final Dictionary<String, String> headers;

    protected final OSGiBundleFile file;

    protected OSGiLoader loader;

    protected int state;

    protected long lastModified;

    protected OSGiBundle(OSGiBundleFile file) throws BundleException {
        this.file = file;
        Manifest mf = file.getManifest();
        if (mf == null) {
            headers = new Hashtable<String,String>();
            symbolicName = file.getFileName();
            version = null;
            return;
        }
        try {
            headers = loadHeaders(file, mf);
        } catch (BundleException e) {
            throw new BundleException("Invalid OSGi Manifest in file " + file
                    + " : " + e.getMessage(), e);
        }
        String name = headers.get(Constants.BUNDLE_SYMBOLICNAME);
        symbolicName = name == null ? file.getFileName() : name;
        version = Version.emptyVersion;
        state = UNINSTALLED;
    }

    protected Dictionary<String, String> loadHeaders(OSGiBundleFile file,
            Manifest mf) throws BundleException {
        return new OSGiManifestReader(file).getHeaders(mf);
    }

    @Override
    public String getLocation() {
        return file.getLocation();
    }

    @Override
    public URL getEntry(String name) {
        return file.getEntry(name);
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern,
            boolean recurse) {
        return file.findEntries(path, filePattern, recurse);
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        return file.getEntryPaths(path);
    }

    @Override
    public long getBundleId() {
        return id;
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return getHeaders(Locale.getDefault().toString());
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        return headers;
    }

    protected String getHeader(String key, String defaultValue) {
        String value = headers.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public boolean hasPermission(Object permission) {
        return true;
    }

    @Override
    public void uninstall() throws BundleException {
        osgi.uninstall(this);
    }

    @Override
    public void update() throws BundleException {
        lastModified = System.currentTimeMillis();
        throw new UnsupportedOperationException(
                "Bundle.update() operations was not yet implemented");
    }

    @Override
    public void update(InputStream in) throws BundleException {
        lastModified = System.currentTimeMillis();
        throw new UnsupportedOperationException(
                "Bundle.update() operations was not yet implemented");
    }

    protected void setInstalled() {
        if (state != UNINSTALLED) {
            throw new IllegalStateException("Not in uninstalled state (" + this
                    + ")");
        }
        lastModified = System.currentTimeMillis();
        state = INSTALLED;
        BundleEvent event = new BundleEvent(BundleEvent.INSTALLED, this);
        osgi.fireBundleEvent(event);
    }

    protected void setUninstalled() {
        if (state != INSTALLED) {
            throw new IllegalStateException("Not in installed state (" + this
                    + ")");
        }
        lastModified = System.currentTimeMillis();
        state = UNINSTALLED;
        BundleEvent event = new BundleEvent(BundleEvent.UNINSTALLED, this);
        osgi.fireBundleEvent(event);
    }

    @Override
    public int hashCode() {
        return symbolicName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bundle) {
            return symbolicName.equals(((Bundle) obj).getSymbolicName());
        }
        return false;
    }

    protected String internalToString() {
        return "id="+ id + ", symbolicName=" + symbolicName
                + ", state=" + state;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + internalToString() + "]";
    }

    @Override
    public int compareTo(Bundle o) {
        return symbolicName.compareTo(o.getSymbolicName());
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
            int signersType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public <A> A adapt(Class<A> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getDataFile(String filename) {
        throw new UnsupportedOperationException();
    }

}
