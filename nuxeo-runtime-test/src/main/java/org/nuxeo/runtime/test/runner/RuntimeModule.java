package org.nuxeo.runtime.test.runner;

import com.google.inject.AbstractModule;

public class RuntimeModule extends AbstractModule {

    /** {@InheritDoc} */
    @Override
    protected void configure() {
        bind(RuntimeHarness.class	).toProvider(RTHarnessProvider.class);
    }

}
