package org.nuxeo.runtime.test.runner;

import com.google.inject.Provider;

public class RTHarnessProvider implements Provider<RuntimeHarness> {
    public RuntimeHarness get() {
        try {
            return NuxeoRunner.getRuntimeHarness();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
