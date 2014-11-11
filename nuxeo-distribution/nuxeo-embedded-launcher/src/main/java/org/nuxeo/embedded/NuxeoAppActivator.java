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
package org.nuxeo.embedded;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;

/**
 * This activator is used by host OSGi frameworks to start the embedded nuxeo.
 * When starting nuxeo from the command line the launching will be done by {@link NuxeoApp}
 * and the activator will do nothing (it will be disabled from {@link NuxeoApp#main(String[])})
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoAppActivator implements BundleActivator {

    private static Log log = LogFactory.getLog(NuxeoAppActivator.class);
    
    
    public void start(BundleContext context) throws Exception {
        if (NuxeoApp.getInstance() == null) {
            File file = getBundleFile(context.getBundle());
            if (file == null) {
                throw new Exception("Cannot locate jar file for bundle: "+context.getBundle().getSymbolicName());
            }
            BundleContextWrapper bc = new BundleContextWrapper(context);
            NuxeoApp.startFramework(bc, new File(NuxeoApp.getDefaultHome()), file, null);
            bc.sendEvent(new FrameworkEvent(FrameworkEvent.STARTED, context.getBundle(), null));
        }
    }
    
    public void stop(BundleContext context) throws Exception {
        NuxeoApp.stopFramework();
    }

    
    public File getBundleFile(Bundle bundle) throws IOException {
        File file;
        String location = bundle.getLocation();
        String vendor = bundle.getBundleContext().getProperty(Constants.FRAMEWORK_VENDOR);
        String name = bundle.getSymbolicName();

        if ("Eclipse".equals(vendor)) { // equinox framework
            return getEclipseBundleFile(bundle);
        } else if (location.startsWith("file:")) { // nuxeo osgi adapter
            try {
                file = FileUtils.urlToFile(location);
            } catch (Exception e) {
                log.error("getBundleFile: Unable to create " +
                        " for bundle: " + name + " as URI: " + location);
                return null;
            }
        } else { // may be a file path - this happens when using JarFileBundle
            // (for ex. in nxshell)
            try {
                file = new File(location);
            } catch (Exception e) {
                log.error("getBundleFile: Unable to create " +
                        " for bundle: " + name + " as file: " + location);
                return null;
            }
        }
        if ((file != null) && file.exists()) {
            log.debug("getBundleFile: " + name +
                    " bound to file: " + file);
            return file;
        } else {
            log.debug("getBundleFile: " + name +
                    " cannot bind to nonexistent file: " + file);
            return null;
        }
    }

    
    protected File getEclipseBundleFile(Bundle bundle) throws IOException {
        try {
            Class<?> klass = bundle.loadClass("org.eclipse.core.runtime.FileLocator");
            try {
                Method m = klass.getMethod("getBundleFile", Bundle.class);
                return (File)m.invoke(null, bundle);
            } catch (NoSuchMethodException e) { // compatibility with eclipse < 3.4
                URL url = resolveEclipseBundleURL(bundle);
                return getEclipseFileFromURL(url);
            }
        } catch (Exception e) {
            IOException ioe = new IOException("failed to get eclipse bundle file location: "+bundle.getLocation());
            ioe.initCause(e);
            throw ioe;
        }        
    }
    
    protected static URL resolveEclipseBundleURL(Bundle bundle) throws IOException {
        try {
            Class<?> klass = bundle.loadClass("org.eclipse.core.runtime.FileLocator");
            Method m = klass.getMethod("resolve", URL.class);
            return (URL)m.invoke(null, bundle.getEntry("/"));
        } catch (Exception e) {
            IOException ioe = new IOException("failed to get eclipse bundle file location: "+bundle.getLocation());
            ioe.initCause(e);
            throw ioe;
        }        
    }
    
    protected static File getEclipseFileFromURL(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) //$NON-NLS-1$
            return new File(url.getPath());
        if ("jar".equals(url.getProtocol())) { //$NON-NLS-1$
            String path = url.getPath();
            if (path.startsWith("file:")) {
                // strip off the file: and the !/
                path = path.substring(5, path.length() - 2);
                return new File(path);
            }
        }
        throw new IOException("Unknown protocol"); //$NON-NLS-1$
    }

    
    
}
