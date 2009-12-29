package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Session;

import com.google.inject.Inject;


@RunWith(NuxeoCoreRunner.class)
@Session(user="Administrator")
@RepositoryFactory(DefaultRepoFactory.class)
public class SimpleSession {

    @Inject
    public CoreSession session;

    @Test
    public void theSessionIsUsable() throws Exception {
        assertNotNull(session);
        assertNotNull(session.getDocument(new PathRef("/default-domain")));
    }

}
