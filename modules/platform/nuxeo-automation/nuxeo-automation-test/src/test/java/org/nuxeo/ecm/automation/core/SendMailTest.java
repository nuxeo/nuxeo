/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.mail.SmtpMailServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, SmtpMailServerFeature.class, AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.notification")
@Deploy("org.nuxeo.ecm.platform.url")
public class SendMailTest {

    protected DocumentModel src;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Inject
    protected SmtpMailServerFeature.MailsResult emailsResult;

    protected OperationContext ctx;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "File");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void testSendMail() throws Exception {
        // add some blobs and then send an email
        Blob blob = Blobs.createBlob("my content");
        blob.setFilename("thefile.txt");

        ctx.setInput(src);
        OperationChain chain = new OperationChain("sendEMail");
        chain.add(FetchContextDocument.ID);
        chain.add(SetDocumentBlob.ID).set("file", blob);
        chain.add(SendMail.ID).set("from", "test@nuxeo.org").set("to", "bs@nuxeo.com").set("subject", "test mail").set(
                "asHTML", true).set("files", "file:content").set(
                "message",
                "<h3>Current doc: ${Document.path}</h3> title: ${Document['dc:title']}<p>Doc link: <a href=\"${docUrl}\">${Document.title}</a>");
        service.run(ctx, chain);
        assertEquals(1, emailsResult.getSize());

        // Check that mandatory headers are present
        emailsResult.assertSender("test@nuxeo.org", 1);
        emailsResult.assertRecipient("bs@nuxeo.com", 1);
        assertTrue(emailsResult.hasSubject("test mail"));
        assertNotNull(emailsResult.getMails().get(0).getDate());
    }
}
