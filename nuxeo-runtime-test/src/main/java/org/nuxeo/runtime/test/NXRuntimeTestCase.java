/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.runtime.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.jmock.Mockery;
import org.junit.runner.RunWith;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * Abstract base class for test cases that require a test runtime service.
 * <p>
 * The runtime service itself is conveniently available as the
 * <code>runtime</code> instance variable in derived classes.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// Make sure this class is kept in sync with with RuntimeHarness
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
public class NXRuntimeTestCase extends TestCase implements RuntimeHarness {

    protected @Inject
    Mockery jmcontext;

    protected @Inject
    RuntimeHarness harness;

    protected @Inject
    RuntimeService runtime;


    @Override
    public boolean isRestart() {
        return harness.isRestart();
    }


    @Override
    public void addWorkingDirectoryConfigurator(
            WorkingDirectoryConfigurator config) {
        harness.addWorkingDirectoryConfigurator(config);
    }

    @Override
    public File getWorkingDir() {
        return harness.getWorkingDir();
    }

    /**
     * Restarts the runtime and preserve homes directory.
     */
    @Override
    public void restart() throws Exception {
        harness.restart();
    }

    @Override
    public void start() throws Exception {
        harness.start();
    }

    /**
     * Fire the event {@code FrameworkEvent.STARTED}.
     */
    @Override
    public void fireFrameworkStarted() throws Exception {
        harness.fireFrameworkStarted();
    }

    @Override
    public void stop() throws Exception {
        harness.stop();
    }

    @Override
    public boolean isStarted() {
        return harness.isStarted();
    }


    @Override
    public void deployContrib(String name, String contrib) throws Exception {
        harness.deployContrib(name, contrib);
    }

    @Override
    public RuntimeContext deployTestContrib(String bundle, String contrib)
            throws Exception {
        return harness.deployTestContrib(bundle, contrib);
    }

    @Override
    public RuntimeContext deployTestContrib(String bundle, URL contrib)
            throws Exception {
        return harness.deployTestContrib(bundle, contrib);
    }

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
    @Override
    public void undeployContrib(String bundle, String contrib) throws Exception {
        harness.undeployContrib(bundle, contrib);
    }

    /**
     * Deploys a whole OSGI bundle.
     * <p>
     * The lookup is first done on symbolic name, as set in
     * <code>MANIFEST.MF</code> and then falls back to the bundle url (e.g.,
     * <code>nuxeo-platform-search-api</code>) for backwards compatibility.
     *
     * @param bundle the symbolic name
     */
    @Override
    public void deployBundle(String name) throws Exception {
        harness.deployBundle(name);
    }

    @Override
    public void deployFolder(File folder, ClassLoader loader) throws Exception {
        harness.deployFolder(folder, loader);
    }

    @Override
    public Properties getProperties() {
        return harness.getProperties();
    }

    @Override
    public RuntimeContext getContext() {
        return harness.getContext();
    }

    @Override
    public OSGiAdapter getOSGiAdapter() {
        return harness.getOSGiAdapter();
    }

    @Override
    public List<String> getClassLoaderFiles() throws URISyntaxException {
        return harness.getClassLoaderFiles();
    }

}
