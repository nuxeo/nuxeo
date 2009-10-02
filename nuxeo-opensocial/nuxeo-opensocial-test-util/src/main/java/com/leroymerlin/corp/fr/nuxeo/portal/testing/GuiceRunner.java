package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.util.List;

import org.junit.internal.runners.InitializationError;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class GuiceRunner extends BlockJUnit4ClassRunner {

    protected Injector injector;
    protected Module[] modules;

    /**
     * Creates a new GuiceTestRunner.
     *
     * @param classToRun
     *            the test class to run
     * @param modules
     *            the Guice modules
     * @throws org.junit.runners.model.InitializationError
     * @throws InitializationError
     *             if the test class is malformed
     */
    public GuiceRunner(final Class<?> classToRun, Module... modules)
            throws org.junit.runners.model.InitializationError {
        super(classToRun);
        this.modules = modules;
        this.injector = Guice.createInjector(modules);
    }

    public void resetInjector() {
        this.injector = Guice.createInjector(modules);
    }

    @Override
    public Object createTest() {
        return injector.getInstance(getTestClass().getJavaClass());
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

}
