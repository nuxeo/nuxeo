package org.nuxeo.runtime.test.runner;

import org.apache.log4j.MDC;
import org.junit.runners.model.FrameworkMethod;

import com.google.inject.Binder;

public class MDCFeature implements RunnerFeature {

    protected static final String F_TEST = "fTest";
    protected static final String F_SUITE = "fSuite";
    protected static final String F_STATE = "fState";

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        MDC.put(F_STATE, "initialize");
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        MDC.put(F_STATE, "configure");
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        MDC.put(F_STATE, "beforeRun");
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        MDC.put(F_STATE, "afterRun");
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        MDC.put(F_STATE, "start");
    }

    @Override
    public void testCreated(Object test) throws Exception {
        MDC.put(F_STATE, "testCreated");
        MDC.put(F_SUITE, test.getClass());
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        MDC.remove(F_STATE);
        MDC.remove(F_SUITE);
        MDC.remove(F_TEST);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        MDC.put(F_STATE, "beforeSetup");
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        MDC.put(F_STATE, "afterTeardown");
    }

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        MDC.put(F_STATE, "beforeMethodRun");
        MDC.put(F_TEST, method.getMethod().getName());
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        MDC.put(F_STATE, "afterMethodRun");
        MDC.remove(F_TEST);
    }

}
