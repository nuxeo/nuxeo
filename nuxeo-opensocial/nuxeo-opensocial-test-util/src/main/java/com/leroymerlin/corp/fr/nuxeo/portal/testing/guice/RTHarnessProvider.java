package com.leroymerlin.corp.fr.nuxeo.portal.testing.guice;

import com.google.inject.Provider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

public class RTHarnessProvider implements Provider<TestRuntimeHarness> {
    public TestRuntimeHarness get() {
        try {
            return NuxeoRunner.getRuntimeHarness();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
