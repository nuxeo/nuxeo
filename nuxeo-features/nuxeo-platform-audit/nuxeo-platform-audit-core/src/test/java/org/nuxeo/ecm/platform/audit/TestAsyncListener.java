/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestAsyncListener {

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    /**
     * This is actually a test of the ReconnectedEventBundle but it needs a session to run so cannot be done in
     * nuxeo-core-event.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.audit.tests:test-async-listener.xml")
    public void testAsyncReconnectedEventBundleOriginatingUser() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("bob", SecurityConstants.READ, true));
        acp.addACL(acl);
        doc.setACP(acp, true);
        session.save();
        DummyAsyncEventListener.actingUser = null;
        // as bob, send an event which is going to be handled by an async listener
        try (CloseableCoreSession s = CoreInstance.openCoreSession(session.getRepositoryName(), "bob")) {
            DocumentModel d = s.getDocument(doc.getRef());
            EventContextImpl ctx = new DocumentEventContext(s, s.getPrincipal(), d);
            eventService.fireEvent(ctx.newEvent("testdummyasync"));
        }
        txFeature.nextTransaction();
        eventService.waitForAsyncCompletion();
        assertEquals("bob", DummyAsyncEventListener.actingUser);
    }

    public static class DummyAsyncEventListener implements PostCommitEventListener {

        protected static String actingUser;

        @Override
        public void handleEvent(EventBundle events) {
            EventContext ctx = events.peek().getContext();
            if (!(ctx instanceof DocumentEventContext)) {
                return;
            }
            CoreSession session = ((DocumentEventContext) ctx).getSourceDocument().getCoreSession();
            actingUser = ((NuxeoPrincipal) session.getPrincipal()).getActingUser();
        }

    }

}
