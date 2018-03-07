/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.MDCFeature;
import org.nuxeo.runtime.test.runner.RandomBug;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * Abstract base class for test cases that require a test runtime service.
 * <p>
 * The runtime service itself is conveniently available as the <code>runtime</code> instance variable in derived
 * classes.
 * <p>
 * <b>Warning:</b> NXRuntimeTestCase subclasses <b>must</b>
 * <ul>
 * <li>not declare they own @Before and @After.
 * <li>override doSetUp and doTearDown (and postSetUp if needed) instead of setUp and tearDown.
 * <li>never call deployXXX methods outside the doSetUp method.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 10.2 this class <b>must</b> not be subclassed anymore, for RuntimeHarness implementation use
 *             {@code RuntimeHarnessImpl}
 */
// Make sure this class is kept in sync with with RuntimeHarness
@RunWith(FeaturesRunner.class)
@Features({ MDCFeature.class, ConditionalIgnoreRule.Feature.class, RandomBug.Feature.class })
@Ignore
@Deprecated
public class NXRuntimeTestCase extends RuntimeHarnessImpl {

    protected Mockery jmcontext = new JUnit4Mockery();

    static {
        // jul to jcl redirection may pose problems (infinite loops) in some
        // environment
        // where slf4j to jul, and jcl over slf4j is deployed
        System.setProperty(AbstractRuntimeService.REDIRECT_JUL, "false");
    }

    private static final Log log = LogFactory.getLog(NXRuntimeTestCase.class);

    protected boolean restart = false;

    protected List<String[]> deploymentStack = new ArrayList<>();

    /**
     * Set to true when the instance of this class is a JUnit test case. Set to false when the instance of this class is
     * instantiated by the FeaturesRunner to manage the framework If the class is a JUnit test case then the runtime
     * components will be started at the end of the setUp method
     */
    protected final boolean isTestUnit;

    /**
     * Used when subclassing to create standalone test cases
     */
    public NXRuntimeTestCase() {
        super();
        isTestUnit = true;
    }

    /**
     * Used by the features runner to manage the Nuxeo framework
     */
    public NXRuntimeTestCase(Class<?> clazz) {
        super(clazz);
        isTestUnit = false;
    }

    /**
     * Restarts the runtime and preserve homes directory.
     */
    @Override
    public void restart() throws Exception {
        restart = true;
        try {
            tearDown();
            setUp();
        } finally {
            restart = false;
        }
    }

    @Override
    public void start() throws Exception {
        startRuntime();
    }

    @Before
    public void startRuntime() throws Exception {
        System.setProperty("org.nuxeo.runtime.testing", "true");
        wipeRuntime();
        initUrls();
        if (urls == null) {
            throw new UnsupportedOperationException("no bundles available");
        }
        initOsgiRuntime();
        setUp(); // let a chance to the subclasses to contribute bundles and/or components
        if (isTestUnit) { // if this class is running as a test case start the runtime components
            fireFrameworkStarted();
        }
        postSetUp();
    }

    /**
     * Implementors should override this method to setup tests and not the {@link #startRuntime()} method. This method
     * should contain all the bundle or component deployments needed by the tests. At the time this method is called the
     * components are not yet started. If you need to perform component/service lookups use instead the
     * {@link #postSetUp()} method
     */
    protected void setUp() throws Exception { // NOSONAR
    }

    /**
     * Implementors should override this method to implement any specific test tear down and not the
     * {@link #stopRuntime()} method
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception { // NOSONAR
        deploymentStack = new ArrayList<>();
    }

    /**
     * Called after framework was started (at the end of setUp). Implementors may use this to use deployed services to
     * initialize fields etc.
     */
    protected void postSetUp() throws Exception { // NOSONAR
    }

    @After
    public void stopRuntime() throws Exception {
        tearDown();
        wipeRuntime();
        if (workingDir != null && !restart) {
            if (workingDir.exists() && !FileUtils.deleteQuietly(workingDir)) {
                log.warn("Cannot delete " + workingDir);
            }
            workingDir = null;
        }
        readUris = null;
        bundles = null;
    }

    @Override
    public void stop() throws Exception {
        stopRuntime();
    }

    protected OSGiRuntimeService handleNewRuntime(OSGiRuntimeService aRuntime) {
        return aRuntime;
    }

    public static URL[] introspectClasspath(ClassLoader loader) {
        return new FastClasspathScanner().getUniqueClasspathElements().stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException cause) {
                throw new Error("Could not get URL from " + file, cause);
            }
        }).toArray(URL[]::new);
    }

    public static URL getResource(String name) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String callerName = Thread.currentThread().getStackTrace()[2].getClassName();
        final String relativePath = callerName.replace('.', '/').concat(".class");
        final String fullPath = loader.getResource(relativePath).getPath();
        final String basePath = fullPath.substring(0, fullPath.indexOf(relativePath));
        Enumeration<URL> resources;
        try {
            resources = loader.getResources(name);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getPath().startsWith(basePath)) {
                    return resource;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return loader.getResource(name);
    }

    protected void deployContrib(URL url) {
        assertEquals(runtime, Framework.getRuntime());
        log.info("Deploying contribution from " + url.toString());
        try {
            runtime.getContext().deploy(url);
        } catch (Exception e) {
            fail("Failed to deploy contrib " + url.toString());
        }
    }

    /**
     * Deploy a contribution specified as a "bundleName:path" uri
     */
    public void deployContrib(String uri) throws Exception {
        int i = uri.indexOf(':');
        if (i == -1) {
            throw new IllegalArgumentException(
                    "Invalid deployment URI: " + uri + ". Must be of the form bundleSymbolicName:pathInBundleJar");
        }
        deployContrib(uri.substring(0, i), uri.substring(i + 1));
    }

    public void undeployContrib(String uri) throws Exception {
        int i = uri.indexOf(':');
        if (i == -1) {
            throw new IllegalArgumentException(
                    "Invalid deployment URI: " + uri + ". Must be of the form bundleSymbolicName:pathInBundleJar");
        }
        undeployContrib(uri.substring(0, i), uri.substring(i + 1));
    }

    protected static boolean isVersionSuffix(String s) {
        if (s.length() == 0) {
            return true;
        }
        return s.matches("-(\\d+\\.?)+(-SNAPSHOT)?(\\.\\w+)?");
    }

    /**
     * Resolves an URL for bundle deployment code.
     * <p>
     * TODO: Implementation could be finer...
     *
     * @return the resolved url
     */
    protected URL lookupBundleUrl(String bundle) { // NOSONAR
        for (URL url : urls) {
            String[] pathElts = url.getPath().split("/");
            for (int i = 0; i < pathElts.length; i++) {
                if (pathElts[i].startsWith(bundle) && isVersionSuffix(pathElts[i].substring(bundle.length()))) {
                    // we want the main version of the bundle
                    boolean isTestVersion = false;
                    for (int j = i + 1; j < pathElts.length; j++) {
                        // ok for Eclipse (/test) and Maven (/test-classes)
                        if (pathElts[j].startsWith("test")) {
                            isTestVersion = true;
                            break;
                        }
                    }
                    if (!isTestVersion) {
                        log.info("Resolved " + bundle + " as " + url.toString());
                        return url;
                    }
                }
            }
        }
        throw new RuntimeServiceException("Could not resolve bundle " + bundle);
    }

    /**
     * Should be called by subclasses after one or more inline deployments are made inside a test method. Without
     * calling this the inline deployment(s) will not have any effects.
     * <p />
     * <b>Be Warned</b> that if you reference runtime services or components you should lookup them again after calling
     * this method!
     * <p />
     * This method also calls {@link #postSetUp()} for convenience.
     */
    protected void applyInlineDeployments() throws Exception {
        runtime.getComponentManager().refresh(false);
        runtime.getComponentManager().start(); // make sure components are started
        postSetUp();
    }

    /**
     * Should be called by subclasses to remove any inline deployments made in the current test method.
     * <p />
     * <b>Be Warned</b> that if you reference runtime services or components you should lookup them again after calling
     * this method!
     * <p />
     * This method also calls {@link #postSetUp()} for convenience.
     */
    protected void removeInlineDeployments() throws Exception {
        runtime.getComponentManager().reset();
        runtime.getComponentManager().start();
        postSetUp();
    }

    /**
     * Hot deploy the given components (identified by an URI). All the started components are stopped, the new ones are
     * registered and then all components are started. You can undeploy these components by calling
     * {@link #popInlineDeployments()}
     * <p>
     * A component URI is of the form: bundleSymbolicName:pathToComponentXmlInBundle
     */
    public void pushInlineDeployments(String... deploymentUris) throws Exception {
        deploymentStack.add(deploymentUris);
        for (String uri : deploymentUris) {
            deployContrib(uri);
        }
        applyInlineDeployments();
    }

    /**
     * Remove the latest deployed components using {@link #pushInlineDeployments(String...)}.
     */
    public void popInlineDeployments() throws Exception {
        if (deploymentStack.isEmpty()) {
            throw new IllegalStateException("deployment stack is empty");
        }
        popInlineDeployments(deploymentStack.size() - 1);
    }

    public void popInlineDeployments(int index) throws Exception {
        if (index < 0 || index > deploymentStack.size() - 1) {
            throw new IllegalStateException("deployment stack index is invalid: " + index);
        }
        deploymentStack.remove(index);

        runtime.getComponentManager().reset();
        for (String[] ar : deploymentStack) {
            for (String element : ar) {
                deployContrib(element);
            }
        }
        applyInlineDeployments();
    }

}
