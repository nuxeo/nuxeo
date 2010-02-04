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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.runtime.test.runner;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.runtime.api.DataSourceHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * JUnit4 runner that can inject classes into the test class.
 * <p>
 * Injection is based on the Guice injection framework.
 */
public class NuxeoRunner extends BlockJUnit4ClassRunner {

    private static final Log log = LogFactory.getLog(NuxeoRunner.class);
    
    /**
     * Runtime harness that holds all the machinery.
     */
    protected static RuntimeHarness harness = new NXRuntimeTestCase();

    /**
     * Guice injector.
     */
    protected Injector injector;
    
    protected DeployScanner deployScanner;

    private static NuxeoRunner currentInstance;

    public NuxeoRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);
        NamingContextFactory.setAsInitial();
        currentInstance = this;
        this.deployScanner = new DeployScanner();
        try {
            deployScanner.load(classToRun);
        } catch (Throwable e) {
            throw new InitializationError(Collections.singletonList(e));
        }
    }
    

    public void resetInjector() {
        this.injector = createInjector();
    }

    protected Injector createInjector() {
        RuntimeModule baseModule = new RuntimeModule();
        // apply overrides from derived classes
        Module module = getCustomModule();
        module = Modules.override(baseModule).with(module);
        // build injector
        return Guice.createInjector(module);    
    }

    @Override
    public Object createTest() {
        // Return a Guice injected test class
        return injector.getInstance(getTestClass().getJavaClass());
    }

    public String[] getBundles() {
        return deployScanner.getBundles().toArray(new String[deployScanner.getBundles().size()]);
    }

    public String[] getLocalResources() {
        return deployScanner.getLocalResources().toArray(new String[deployScanner.getBundles().size()]);
    }

    /**
     * Deploys bundles specified in the @Bundles annotation.
     */
    protected void deployTestClassBundles() throws Exception {
        deploy(); // deploy additional bundles from derived runners
        String[] bundles = getBundles();
        if (bundles.length > 0) {
            try {
                harness = getRuntimeHarness();
                for (String bundle : bundles) {
                    int p = bundle.indexOf(':');
                    if (p == -1) {
                        harness.deployBundle(bundle);
                    } else {
                        harness.deployContrib(bundle.substring(0, p), bundle.substring(p+1));
                    }
                }
            } catch (Exception e) {
                log.error("Unable to start bundles: " + bundles);
            }
        }
        String[] localResources = getLocalResources();
        if (localResources.length > 0) {
            try {
                harness = getRuntimeHarness();
                for (String bundle : localResources) {
                    int p = bundle.indexOf(':');
                    if (p == -1) {
                        throw new IllegalArgumentException("Local resources must specify a traget bundle. "+bundle);
                    } else {
                        URL url = getClass().getClassLoader().getResource(bundle.substring(p+1));
                        harness.deployTestContrib(bundle.substring(0, p), url);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to start bundles: " + bundles);
            }
        }        
    }

    @Override
    protected void validateZeroArgConstructor(List<Throwable> errors) {
        // Guice can inject constructors with parameters so we don't want this
        // method to trigger an error
    }

    /**
     * Gets the Guice injector.
     */
    protected Injector getInjector() {
        return injector;
    }

    /**
     * Gets the current {@link NuxeoRunner} instance.
     * <p>
     * Can be useful in test class in order to reset things.
     */
    public static NuxeoRunner getInstance() {
        return currentInstance;
    }

    /**
     * Gets the harness used by the Nuxeo Runner (only used by the
     * {@link RuntimeHarnessProvider})
     */
    protected static RuntimeHarness getRuntimeHarness() {
        return harness;
    }

    @Override
    public void run(final RunNotifier notifier) {
        try {
            try {
                // Starts Nuxeo Runtime
                if (!harness.isStarted()) {
                    harness.start();
                    initialize();
                }
                // Deploy additional bundles
                deployTestClassBundles();
                harness.fireFrameworkStarted();
                // injector must be created now - to have all services registered
                this.injector = createInjector();
                beforeRun();
                // Runs the class
                super.run(notifier);
                afterRun();
            } finally {
                // Stops the harness if needed
                if (harness.isStarted()) {
                    cleanup();
                    harness.stop();
                }
            }
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }

    }
    
    /**
     * Do any initialization needed like setup JNDI bindings setup, FileSystem setup etc.
     * This is called after runtime is started and before deploying bundles 
     * @throws Exception
     */
    protected void initialize() throws Exception {
        
    }

    /**
     * Do cleanup. This is called after test stopped and before stopping nuxeo runtime.
     * @throws Exception
     */
    protected void cleanup() throws Exception {
        
    }
    
    /**
     * Runs once before the Nuxeo tests of this runner.
     */
    protected void beforeRun() throws Exception {
    }

    /**
     * Runs once after the Nuxeo tests of this runner.
     */
    protected void afterRun() throws Exception {
    }

    /**
     * Configure modules inherited from derived classes
     */
    protected Module getCustomModule() {
        Module module = new Module() {
            public void configure(Binder arg0) {
                NuxeoRunner.this.configure(arg0);
            }
        };
        return module;
    }
    
    protected void configure(Binder binder) {
        // do nothing
    }

    /**
     * Default bundles to deploy. Override this to specify default bundles.
     * To deploy a bundle from annotations use {@link #scanDeployments(Class)}.
     * Example:
     * <pre>
     * protected void deploy() throws Exception {
     *   scanDeployments(CoreDeployments.class);
     * }
     * </pre>
     * 
     */
    protected void deploy() throws Exception {
        
    }

    public DeployScanner getDeployScanner() {
        return deployScanner;
    }
    
    protected void scanDeployments(Class<?> clazz) throws Exception {
        deployScanner.load(clazz);
    }
        
    public static void bindDatasource(String key, DataSource ds) throws Exception {
        InitialContext initialCtx = new InitialContext();        
        JndiHelper.rebind(initialCtx, DataSourceHelper.getDataSourceJNDIName(key), ds);
    }

}
