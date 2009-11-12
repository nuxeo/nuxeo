/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 * $Id$
 */
package org.nuxeo.runtime.test.runner;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * jUnit4 runner that can Inject class into the test class Injection is based on
 * the Guice injection framework
 *
 * @author dmetzler
 *
 */
public class NuxeoRunner extends BlockJUnit4ClassRunner {

    private static final Logger LOG = Logger.getLogger(NuxeoRunner.class);

    /**
     * Runtime harness that holds all the machinery
     */
    protected static RuntimeHarness harness = new RuntimeHarness();

    /**
     * Guice modules to create the injector
     */
    protected Module[] modules;

    /**
     * Guice injector
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
        if (annotation != null) {
            return annotation.value();
        } else {
            return new String[0];
        }
    }

    /**
     * Deploy bundles specified in the @Bundles annotation
     */
    private void deployTestClassBundles() {
        String[] bundles = getBundles();
        if (bundles.length > 0) {
            try {
                harness = getRuntimeHarness();
                for (String bundle : bundles) {
                    harness.deployBundle(bundle);
                }
            } catch (Exception e) {
                LOG.error("Unable to start bundles: " + bundles);
            }
        }

    }

    @Override
    protected void validateZeroArgConstructor(List<Throwable> errors) {
        // Guice can inject constructors with parameters so we don't want this
        // method to trigger an error
    }

    /**
     * Returns the Guice injector.
     *
     * @return the Guice injector
     */
    protected Injector getInjector() {
        return injector;
    }

    /**
     * Can be useful in test class in order to reset things
     *
     * @return
     */
    public static NuxeoRunner getInstance() {
        return currentInstance;
    }


    /**
     * Returns the harness used by the Nuxeo Runner (only used by the
     * RTHarnessProvider)
     *
     * @return
     * @throws Exception
     */
    static RuntimeHarness getRuntimeHarness() throws Exception {
        return harness;
    }

    @Override
    public void run(final RunNotifier notifier) {
        try {
            // Starts Nuxeo Runtim
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

    public void beforeRun() {

    }
    public void afterRun() {

    }
}
