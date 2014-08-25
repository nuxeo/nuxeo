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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;

import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.osgi.OSGiAdapter;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

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
@Ignore
@Features(RuntimeFeature.class)
public abstract class NXRuntimeTestCase implements RuntimeHarness {

    protected @Inject Mockery jmcontext;

    protected @Inject RuntimeHarness harness;

    protected @Inject RuntimeService runtime;

    protected Set<RegistrationInfo> discovered = new HashSet<RegistrationInfo>();

    @Before
    public void setUp() throws Exception {
        runtime.getComponentManager().addComponentListener(
                new ComponentListener() {

                    @Override
                    public void handleEvent(ComponentEvent event) {
                        switch (event.id) {
                        case ComponentEvent.COMPONENT_REGISTERED: {
                            discovered.add(event.registrationInfo);
                            break;
                        }
                        case ComponentEvent.COMPONENT_UNREGISTERED: {
                            discovered.remove(event.registrationInfo);
                        }
                        }
                    }
                });

    }

    @After
    public void tearDown() throws Exception {
        for (RegistrationInfo info : discovered.toArray(new RegistrationInfo[discovered.size()])) {
            info.getContext().undeploy(info.getXmlFileUrl());
        }
        Assert.assertTrue(discovered.isEmpty());
    }

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
        for (RegistrationInfo info : discovered) {
            info.notifyApplicationStarted();
        }
    }

    @Override
    public void stop() throws Exception {
        harness.stop();
    }

    @Override
    public boolean isStarted() {
        return harness.isStarted();
    }

    /**
     * Deploys a contribution from a given bundle.
     * <p>
     * The path will be relative to the bundle root. Example: <code>
     * deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml")
     * </code>
     * <p>
     * For compatibility reasons the name of the bundle may be a jar name, but
     * this use is discouraged and deprecated.
     *
     * @param bundle the name of the bundle to peek the contrib in
     * @param contrib the path to contrib in the bundle.
     */
    @Override
    public void deployContrib(String name, String contrib) throws Exception {
        harness.deployContrib(name, contrib);
    }

    /**
     * Deploy an XML contribution from outside a bundle.
     * <p>
     * This should be used by tests wiling to deploy test contribution as part
     * of a real bundle.
     * <p>
     * The bundle owner is important since the contribution may depend on
     * resources deployed in that bundle.
     * <p>
     * Note that the owner bundle MUST be an already deployed bundle.
     *
     * @param bundle the bundle that becomes the contribution owner
     * @param contrib the contribution to deploy as part of the given bundle
     */
    @Override
    public RuntimeContext deployTestContrib(String name, String contrib)
            throws Exception {
        return harness.deployTestContrib(name, contrib);
    }

    @Override
    public RuntimeContext deployTestContrib(String name, URL contrib)
            throws Exception {
        return harness.deployTestContrib(name, contrib);
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
    public void undeployContrib(String name, String contrib) throws Exception {
        harness.undeployContrib(name, contrib);
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

    @Override
    public void undeployContrib(String contrib) {
        harness.undeployContrib(contrib);
    }

    @Override
    public void undeploy(String contrib) {
        harness.undeploy(contrib);
    }

    @Override
    public void deployContrib(String contrib) {
        harness.deployContrib(contrib);
    }

    @Override
    public void deploy(String contrib) {
        harness.deploy(contrib);
    }

}
