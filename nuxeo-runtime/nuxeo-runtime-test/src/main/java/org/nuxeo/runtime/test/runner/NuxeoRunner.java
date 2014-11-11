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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
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
    protected RuntimeHarness harness;

    protected static AnnotationScanner scanner = new AnnotationScanner();
    
    
    /**
     * Guice injector.
     */
    protected Injector injector;
    
    protected DeploymentSet deploy;
    
    protected List<RunnerFeature> features;

    

    public NuxeoRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);        
        NamingContextFactory.setAsInitial();
        harness = new NXRuntimeTestCase();
        try {
            loadFeatures(classToRun);
        } catch (Throwable e) {
            throw new InitializationError(Collections.singletonList(e));
        }
    }
    
    protected void loadFeature(LinkedHashSet<Class<? extends RunnerFeature>> features, Class<? extends RunnerFeature> clazz) throws Exception {
        if (features.contains(clazz)) {
            return;
        }
        features.add(clazz);
        scanner.scan(clazz);
        // load required features from annotation
        List<Features> annos = scanner.getAnnotations(clazz, Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    if (!features.contains(cl)) {
                        loadFeature(features, cl);
                    }
                }
            }
        }
        // load deployments from annotation
        deploy.load(scanner, clazz);
    }
    
    protected void loadFeatures(Class<?> classToRun) throws Exception {
        scanner.scan(classToRun);
        deploy = new DeploymentSet();
        LinkedHashSet<Class<? extends RunnerFeature>> features = new LinkedHashSet<Class<? extends RunnerFeature>>();
        // load required features from annotation
        List<Features> annos = scanner.getAnnotations(classToRun, Features.class);
        if (annos != null) {
            for (Features anno : annos) {
                for (Class<? extends RunnerFeature> cl : anno.value()) {
                    if (!features.contains(cl)) {
                        loadFeature(features, cl);
                    }
                }
            }
        }
        // initialize features
        this.features = new ArrayList<RunnerFeature>();
        for (Class<? extends RunnerFeature> fc : features) {
            RunnerFeature rf = fc.newInstance();
            rf.initialize(this, classToRun);
            this.features.add(rf);
        }
        // load deployments from class to run
        deploy.load(scanner, classToRun);
    }
    
    public <T extends RunnerFeature> T getFeature(Class<T> type) {
        for (RunnerFeature rf : features) {
            if (rf.getClass() == type) {
                return type.cast(rf);
            }
        }
        return null;
    }
    
    protected void fireCleanup() throws Exception {
        for (RunnerFeature feature : features) {
            feature.cleanup(this);
        }
    }

    protected void fireBeforeRun() throws Exception {
        for (RunnerFeature feature : features) {
            feature.beforeRun(this);
        }
    }

    protected void fireAfterRun() throws Exception {
        for (RunnerFeature feature : features) {
            feature.afterRun(this);
        }
    }

    protected void fireConfigure(Binder binder) {
        for (RunnerFeature feature : features) {
            feature.configure(this, binder);
        }
    }

    protected void fireDeploy() throws Exception {
        for (RunnerFeature feature : features) {
            feature.deploy(this);
        }
    }
    
    protected void fireBeforeMethodRun(FrameworkMethod method, Object test) throws Exception {
        for (RunnerFeature feature : features) {
            feature.beforeMethodRun(this, method, test);
        }        
    }
    
    protected void fireAfterMethodRun(FrameworkMethod method, Object test) throws Exception {
        for (RunnerFeature feature : features) {
            feature.afterMethodRun(this, method, test);
        }    
    }


    public static AnnotationScanner getScanner() {
        return scanner;
    }
    
    public List<RunnerFeature> getFeatures() {
        return features;
    }

    public void resetInjector() {
        this.injector = createInjector();
    }
    
    public RuntimeHarness getHarness() {
        return harness;
    }

    protected Injector createInjector() {
        RuntimeModule baseModule = new RuntimeModule(this);
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

    public String[] getDeployments() {
        return deploy.getDeployments().toArray(new String[deploy.getDeployments().size()]);
    }

    public String[] getLocalDeployments() {
        return deploy.getLocalDeployments().toArray(new String[deploy.getLocalDeployments().size()]);
    }

    /**
     * Deploys bundles specified in the @Bundles annotation.
     */
    protected void deployTestClassBundles() throws Exception {
        fireDeploy(); // deploy additional bundles from features
        String[] bundles = getDeployments();
        if (bundles.length > 0) {
            try {
                harness = getHarness();
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
        String[] localResources = getLocalDeployments();
        if (localResources.length > 0) {
            try {
                harness = getHarness();
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
    public Injector getInjector() {
        return injector;
    }

    @Override
    public void run(final RunNotifier notifier) {
        try {
            try {
                // Starts Nuxeo Runtime
                if (!harness.isStarted()) {
                    harness.start();
                }
                // Deploy additional bundles
                deployTestClassBundles();
                harness.fireFrameworkStarted();
                // injector must be created now - to have all services registered
                this.injector = createInjector();
                beforeRun();
                // Runs the class
                try {
                    super.run(notifier);
                } finally {
                    afterRun();
                }
            } finally {
                // Stops the harness if needed
                if (harness.isStarted()) {
                    cleanup();
                    harness.stop();
                    //harness = null;
                }
            }
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }

    }

    /**
     * Do cleanup. This is called after test stopped and before stopping nuxeo runtime.
     * @throws Exception
     */
    protected void cleanup() throws Exception {
        fireCleanup();
    }
    
    /**
     * Runs once before the Nuxeo tests of this runner.
     */
    protected void beforeRun() throws Exception {
        fireBeforeRun();
    }

    /**
     * Runs once after the Nuxeo tests of this runner.
     */
    protected void afterRun() throws Exception {
        fireAfterRun();
    }

    /**
     * Configure modules inherited from derived classes
     */
    protected Module getCustomModule() {
        Module module = new Module() {
            public void configure(Binder arg0) {
                fireConfigure(arg0);
            }
        };
        return module;
    }

    /**
     * Used to 
     */
    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return new InvokeMethod(method, test);
    }
    
    public static void bindDatasource(String key, DataSource ds) throws Exception {
        InitialContext initialCtx = new InitialContext();        
        JndiHelper.rebind(initialCtx, DataSourceHelper.getDataSourceJNDIName(key), ds);
    }
    
    protected class InvokeMethod extends Statement {
        protected FrameworkMethod testMethod;
        protected Object target;        
        public InvokeMethod(FrameworkMethod testMethod, Object target) {
            this.testMethod= testMethod;
            this.target= target;
        }        
        public void evaluate() throws Throwable {
            fireBeforeMethodRun(testMethod, target);
            try {
                testMethod.invokeExplosively(target);
            } finally {
                fireAfterMethodRun(testMethod, target);    
            }
        }
    }

}
