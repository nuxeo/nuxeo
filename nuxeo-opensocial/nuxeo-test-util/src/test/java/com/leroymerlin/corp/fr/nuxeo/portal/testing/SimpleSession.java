package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PathRef;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation.RepositoryFactory;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.annotation.Session;


@RunWith(NuxeoRunner.class)
@Session(user="Administrator")
@RepositoryFactory(SimpleRepoFactory.class)
public class SimpleSession {

    @Inject
    public CoreSession session;

    @Test
    public void theSessionIsUsable() throws Exception {
        assertNotNull(session);
        assertNotNull(session.getDocument(new PathRef("/test")));
    }

}
