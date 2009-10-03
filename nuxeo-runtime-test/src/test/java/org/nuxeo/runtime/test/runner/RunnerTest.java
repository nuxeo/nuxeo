package org.nuxeo.runtime.test.runner;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;


@RunWith(NuxeoRunner.class)
@Bundles({"org.nuxeo.runtime.jetty"})
public class RunnerTest {

    @Inject
    RuntimeHarness harness;

    @Test
    public void jettyComponentIsDeployed() throws Exception {
        assertNotNull(Framework.getService(org.mortbay.jetty.Server.class));
    }

}