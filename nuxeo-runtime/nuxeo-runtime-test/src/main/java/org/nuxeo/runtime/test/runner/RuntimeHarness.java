/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.net.URL;

import org.nuxeo.osgi.BundleFile;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * Move this to org.nuxeo.runtime package
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface RuntimeHarness {
    
    /**
     * Get the framework working directory
     * @return
     */
    public File getWorkingDir();
    
    /**
     * Fire the event {@code FrameworkEvent.STARTED}.
     */
    public void fireFrameworkStarted() throws Exception;
 
    /**
     * Deploys a whole OSGI bundle.
     * <p>
     * The lookup is first done on symbolic name, as set in <code>MANIFEST.MF</code>
     * and then falls back to the bundle url (e.g., <code>nuxeo-platform-search-api</code>)
     * for backwards compatibility.
     *
     * @param bundle the symbolic name
     */
    public void deployBundle(String bundle) throws Exception;
    
    /**
     * Undeploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root.
     * Example:
     * <code>
     * undeployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     *
     * @param bundle the bundle
     * @param contrib the contribution
     */
    public void undeployContrib(String bundle, String contrib) throws Exception;
    
    /**
     * @deprecated use {@link #undeployContrib(String, String)} instead
     */
    @Deprecated
    public void undeployContrib(String contrib);
        

    /**
     * @deprecated use {@link #undeployContrib(String, String)} instead
     */
    @Deprecated
    public void undeploy(String contrib);
    
    public RuntimeContext deployTestContrib(String bundle, URL contrib) throws Exception;
    
    /**
     * Deploy an XML contribution from outside a bundle.
     * <p>
     * This should be used by tests
     * wiling to deploy test contribution as part of a real bundle.
     * <p>
     * The bundle owner is important since the contribution may depend on resources
     * deployed in that bundle.
     * <p>
     * Note that the owner bundle MUST be an already deployed bundle.
     *
     * @param bundle the bundle that becomes the contribution owner
     * @param contrib the contribution to deploy as part of the given bundle
     */
    public RuntimeContext deployTestContrib(String bundle, String contrib) throws Exception;
    
    /**
     * Deploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root.
     * Example:
     * <code>
     * deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     * <p>
     * For compatibility reasons the name of the bundle may be a jar name, but
     * this use is discouraged and deprecated.
     *
     * @param bundle the name of the bundle to peek the contrib in
     * @param contrib the path to contrib in the bundle.
     */
    public void deployContrib(String bundle, String contrib) throws Exception;
    
    /**
     * Deploys a contribution file by looking for it in the class loader.
     * <p>
     * The first contribution file found by the class loader will be used.
     * You have no guarantee in case of name collisions.
     *
     * @deprecated use the less ambiguous {@link #deployContrib(BundleFile,String)}
     * @param contrib the relative path to the contribution file
     */
    @Deprecated
    public void deployContrib(String contrib);
    
    /**
     * @deprecated use <code>deployContrib()</code> instead
     */
    @Deprecated
    public void deploy(String contrib);
    
    public void start() throws Exception;
    
    public void stop() throws Exception;

    public boolean isStarted();
    
    public void deployFolder(File folder, ClassLoader loader) throws Exception;
}
