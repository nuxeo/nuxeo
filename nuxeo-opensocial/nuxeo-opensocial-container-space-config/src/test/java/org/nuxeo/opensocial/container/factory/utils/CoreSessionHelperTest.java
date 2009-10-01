package org.nuxeo.opensocial.container.factory.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class CoreSessionHelperTest {

    private CoreSession session;

    @Inject
    public CoreSessionHelperTest(TestRuntimeHarness harness, CoreSession session)
            throws Exception {
        assertNotNull(session);
        this.session = session;

    }

    @Test
    public void iCanGetSessionWithRepositoryName() throws Exception {
        CoreSession sessionTest = CoreSessionHelper.getCoreSession(session.getRepositoryName());

        assertFalse(sessionTest.getSessionId().equals(session.getSessionId()));
        assertNotNull(sessionTest);

    }
}
