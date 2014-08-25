package org.nuxeo.runtime.test.runner;

import org.apache.log4j.MDC;
import org.junit.runners.model.FrameworkMethod;

public class LogContextFeature extends SimpleFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        MDC.put("fClass", runner.getTargetTestClass().getName());
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        MDC.remove("fClass");
    }

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        MDC.put("fMethod", method.getName());
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method,
            Object test) throws Exception {
        MDC.remove("fMethod");
    }

}
