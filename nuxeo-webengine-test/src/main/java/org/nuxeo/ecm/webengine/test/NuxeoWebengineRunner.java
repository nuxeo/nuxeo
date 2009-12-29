package org.nuxeo.ecm.webengine.test;

import org.junit.runners.model.InitializationError;
import org.nuxeo.ecm.core.test.guice.CoreModule;
import org.nuxeo.ecm.platform.test.NuxeoPlatformRunner;
import org.nuxeo.ecm.platform.test.PlatformModule;
import org.nuxeo.runtime.test.runner.RuntimeModule;

import com.google.inject.Module;

public class NuxeoWebengineRunner extends NuxeoPlatformRunner {
    public NuxeoWebengineRunner(Class<?> classToRun) throws InitializationError {
        // FIXME: There's surely a better way to inherit from parent modules...
        this(classToRun, new RuntimeModule(), new CoreModule(),
                new PlatformModule(), new WebengineModule());
    }

    public NuxeoWebengineRunner(Class<?> classToRun, Module... modules)
            throws InitializationError {
        super(classToRun, modules);
    }

}
