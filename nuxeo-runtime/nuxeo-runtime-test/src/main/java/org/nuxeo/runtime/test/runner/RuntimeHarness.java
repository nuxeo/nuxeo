/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;

/**
 * TODO: Move this to org.nuxeo.runtime package
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface RuntimeHarness {

    /**
     * Gets the framework working directory.
     */
    File getWorkingDir();

    /**
     * Fires the event {@code FrameworkEvent.STARTED}.
     */
    void fireFrameworkStarted() throws Exception;

    /**
     * Deploys a whole OSGI bundle.
     * <p>
     * The lookup is first done on symbolic name, as set in <code>MANIFEST.MF</code> and then falls back to the bundle
     * url (e.g., <code>nuxeo-platform-search-api</code>) for backwards compatibility.
     *
     * @param bundle the symbolic name
     */
    void deployBundle(String bundle) throws Exception;

    /**
     * Undeploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
     * undeployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     *
     * @param bundle the bundle
     * @param contrib the contribution
     */
    void undeployContrib(String bundle, String contrib) throws Exception;

    RuntimeContext deployTestContrib(String bundle, URL contrib) throws Exception;

    /**
     * Deploys an XML contribution from outside a bundle.
     * <p>
     * This should be used by tests wiling to deploy test contribution as part of a real bundle.
     * <p>
     * The bundle owner is important since the contribution may depend on resources deployed in that bundle.
     * <p>
     * Note that the owner bundle MUST be an already deployed bundle.
     *
     * @param bundle the bundle that becomes the contribution owner
     * @param contrib the contribution to deploy as part of the given bundle
     */
    RuntimeContext deployTestContrib(String bundle, String contrib) throws Exception;

    /**
     * Deploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
     * deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     * <p>
     * For compatibility reasons the name of the bundle may be a jar name, but this use is discouraged and deprecated.
     *
     * @param bundle the name of the bundle to peek the contrib in
     * @param contrib the path to contrib in the bundle.
     */
    void deployContrib(String bundle, String contrib) throws Exception;

    void start() throws Exception;

    void stop() throws Exception;

    boolean isStarted();

    void deployFolder(File folder, ClassLoader loader) throws Exception;

    void addWorkingDirectoryConfigurator(WorkingDirectoryConfigurator config);

    /**
     * Framework properties for variable injections
     *
     * @since 5.4.2
     */
    Properties getProperties();

    /**
     * Runtime context for deployment
     *
     * @since 5.4.2
     */
    RuntimeContext getContext();

    /**
     * OSGI bridge
     *
     * @since 5.4.2
     */
    OSGiAdapter getOSGiAdapter();

    /**
     * @since 5.5
     */
    boolean isRestart();

    /**
     * @since 5.5
     * @throws Exception
     */
    void restart() throws Exception;

    /**
     * @throws URISyntaxException
     * @since 5.7
     */
    List<String> getClassLoaderFiles() throws URISyntaxException;

}
