package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryFactory;

import com.google.inject.Inject;


@RunWith(NuxeoCoreRunner.class)
@RepositoryFactory(DefaultRepoFactory.class)
public class DefaultRepoFactoryTest {
    @Inject CoreSession session;

    @Test
    public void testname() throws Exception {
        assertTrue(session.exists(new PathRef("/default-domain/workspaces")));
    }
}
