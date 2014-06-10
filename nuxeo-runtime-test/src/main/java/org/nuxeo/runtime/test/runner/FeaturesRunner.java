/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import com.google.inject.Injector;

/**
 * A Test Case runner that can be extended through features and provide
 * injection though Guice.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FeaturesRunner extends BlockJUnit4ClassRunner {

    public FeaturesRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);
    }

    public Class<?> getTargetTestClass() {
        return super.getTestClass().getJavaClass();
    }

    public AnnotationScanner getScanner() {
        throw new UnsupportedOperationException();
    }


    public <T extends RunnerFeature> T getFeature(Class<T> type) {
        throw new UnsupportedOperationException();
    }

    public List<RunnerFeature> getFeatures() {
        throw new UnsupportedOperationException();
    }


    /**
     * @since 5.6
     */
    public <T extends Annotation> T getConfig(Class<T> type) {
        throw new UnsupportedOperationException();
    }


    /**
     * Get the annotation on the test method, if no annotation has been found,
     * get the annotation from the test class (See {@link #getConfig(Class)})
     *
     * @since 5.7
     */
    public <T extends Annotation> T getConfig(FrameworkMethod method,
            Class<T> type) {
        throw new UnsupportedOperationException();
    }

    public Injector getInjector() {
        return injector;
    }

    public void resetInjector() {
        injector = createInjector();
    }

    protected Injector createInjector() {
        Module module = new Module() {
            @Override
            public void configure(Binder arg0) {
                configureBindings(arg0);
            }
        };
        // build injector
        return Guice.createInjector(module);
    }

    @Override
    public void run(final RunNotifier notifier) {
        AssertionError errors = new AssertionError("features error");
        try {
            try {
                start();
                try {
                    beforeRun();
                    resetInjector();
                    super.run(notifier); // launch tests
                } catch (Exception error) {
                    errors.addSuppressed(error);
                } finally {
                    afterRun();
                }
            } catch (Exception error) {
                errors.addSuppressed(error);
            } finally {
                try {
                    stop();
                } catch (Exception error) {
                    error.addSuppressed(errors);
                }
            }
        } catch (Throwable error) {
            errors.addSuppressed(error);
        } finally {
            if (errors.getSuppressed().length > 0) {
                notifier.fireTestFailure(new Failure(getDescription(),
                        errors));
            }
        }
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        if (method.getAnnotation(Ignore.class) != null) {
            super.runChild(method, notifier);
            return;
        }
        beforeSetup();
        try {
            super.runChild(method, notifier);
        } finally {
            afterTeardown();
        }
    }

    @Override
    public Object createTest() throws Exception {
        // Return a Guice injected test class
        Object test = injector.getInstance(getTestClass().getJavaClass());
        // let features adapt the test object if needed
        try {
            testCreated(test);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare test instance: "
                    + test, e);
        }
        return test;
    }

    @Override
    protected void validateZeroArgConstructor(List<Throwable> errors) {
        // Guice can inject constructors with parameters so we don't want this
        // method to trigger an error
        throw new UnsupportedOperationException();
    }

    @Override
    public void run(RunNotifier notifier) {
        if (FeaturesRunner.class.equals(getClass())) {
            new OSGiTestLoader().run(getTargetTestClass(), notifier);
        } else {
            super.run(notifier);
        }
    }

    @Override
    public String toString() {
        return "FeaturesRunner [fTest=" + getTargetTestClass() + "]";
    }

}
