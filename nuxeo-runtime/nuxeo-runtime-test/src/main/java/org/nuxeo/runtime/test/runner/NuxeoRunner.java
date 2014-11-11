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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

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
    protected static RuntimeHarness harness = new RuntimeHarness();

    /**
     * Guice modules to create the injector.
     */
    protected Module[] modules;

    /**
     * Guice injector.
     */
    protected Injector injector;

    private static NuxeoRunner currentInstance;

    public NuxeoRunner(Class<?> classToRun) throws InitializationError {
        this(classToRun, new RuntimeModule());
    }

    public NuxeoRunner(Class<?> classToRun, Module... modules)
            throws InitializationError {
        super(classToRun);
        currentInstance = this;

        // Create the Guice injector, based on modules
        this.modules = modules;
        this.injector = Guice.createInjector(modules);
    }

    public void resetInjector() {
        this.injector = Guice.createInjector(modules);
    }

    @Override
    public Object createTest() {
        // Return a Guice injected test class
        return injector.getInstance(getTestClass().getJavaClass());
    }

    public String[] getBundles() {
        Bundles annotation = getDescription().getAnnotation(Bundles.class);
        return annotation == null ? new String[0] : annotation.value();
    }

    /**
     * Deploys bundles specified in the @Bundles annotation.
     */
    protected void deployTestClassBundles() {
        String[] bundles = getBundles();
        if (bundles.length > 0) {
            try {
                harness = getRuntimeHarness();
                for (String bundle : bundles) {
                    harness.deployBundle(bundle);
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
    protected static RuntimeHarness getRuntimeHarness() throws Exception {
        return harness;
    }

    @Override
    public void run(final RunNotifier notifier) {
        try {
            // Starts Nuxeo Runtime
            harness.start();

            // Deploy additional bundles
            deployTestClassBundles();

            beforeRun();
            // Runs the class
            super.run(notifier);
            afterRun();

            // Stops the harness if needed
            if (harness.isStarted()) {
                harness.stop();
            }
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        }

    }

    /**
     * Runs once before the Nuxeo tests of this runner.
     */
    protected void beforeRun() {
    }

    /**
     * Runs once after the Nuxeo tests of this runner.
     */
    protected void afterRun() {
    }

}
