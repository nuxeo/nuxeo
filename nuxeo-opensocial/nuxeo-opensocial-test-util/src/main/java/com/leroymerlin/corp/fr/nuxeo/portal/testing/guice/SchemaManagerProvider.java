package com.leroymerlin.corp.fr.nuxeo.portal.testing.guice;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

public class SchemaManagerProvider implements Provider<SchemaManager>{


    private final TestRuntimeHarness harness;

    @Inject
    public SchemaManagerProvider(TestRuntimeHarness harness) {
        this.harness = harness;

    }

    public SchemaManager get() {
        try {
            harness.deployBundle("org.nuxeo.ecm.core.schema");
            return Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

    }

}
