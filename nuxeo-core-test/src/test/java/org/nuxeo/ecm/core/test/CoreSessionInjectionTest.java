package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;

import com.google.inject.Inject;

@RunWith(NuxeoCoreRunner.class)
public class CoreSessionInjectionTest {
    @Inject CoreSession session;

    @Test
    public void iCanAccessTheSession() throws Exception {
        assertNotNull(session);
    }
}
