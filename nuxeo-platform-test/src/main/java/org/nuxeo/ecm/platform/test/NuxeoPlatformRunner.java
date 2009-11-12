package org.nuxeo.ecm.platform.test;

import org.junit.runners.model.InitializationError;
import org.nuxeo.ecm.core.test.NuxeoCoreRunner;
import org.nuxeo.ecm.core.test.guice.CoreModule;
import org.nuxeo.runtime.test.runner.RuntimeModule;

import com.google.inject.Module;

public class NuxeoPlatformRunner extends NuxeoCoreRunner {

    public NuxeoPlatformRunner(Class<?> classToRun) throws InitializationError {
        //FIXME: There's surely a better way to inherit from parent modules...
        this(classToRun, new RuntimeModule(), new CoreModule(), new PlatformModule());
    }

    public NuxeoPlatformRunner(Class<?> classToRun, Module... modules)
            throws InitializationError {
        super(classToRun, modules);
    }



}
