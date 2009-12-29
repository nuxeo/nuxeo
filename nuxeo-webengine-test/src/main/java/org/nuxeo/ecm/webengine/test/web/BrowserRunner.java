package org.nuxeo.ecm.webengine.test.web;

import java.util.Arrays;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;
import org.nuxeo.ecm.core.test.guice.CoreModule;
import org.nuxeo.ecm.platform.test.NuxeoPlatformRunner;
import org.nuxeo.ecm.platform.test.PlatformModule;
import org.nuxeo.ecm.webengine.test.WebengineModule;
import org.nuxeo.runtime.test.runner.RuntimeModule;

public class BrowserRunner extends NuxeoPlatformRunner {

    public BrowserRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun, new RuntimeModule(), new CoreModule(),
                new PlatformModule(), new WebengineModule(), new BrowserModule());
        try {
            BrowserConfig config = getInjector().getInstance(
                    BrowserConfig.class);
            final String browserType = config.getBrowser();
            filter(new Filter() {

                @Override
                public boolean shouldRun(Description description) {
                    SkipBrowser skip = description
                            .getAnnotation(SkipBrowser.class);
                    if (skip != null
                            && Arrays.asList(skip.value())
                                    .contains(browserType)) {

                        return false;
                    }
                    return true;
                }

                @Override
                public String describe() {
                    return "Filtering tests according to current browser settings";
                }
            });
        } catch (ClassCastException e) {
            // OK - just skip
        } catch (NoTestsRemainException e) {
            e.printStackTrace();
        }
    }
}
