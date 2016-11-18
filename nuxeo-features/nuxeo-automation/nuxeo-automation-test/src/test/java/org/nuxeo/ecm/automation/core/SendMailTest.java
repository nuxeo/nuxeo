/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import com.dumbster.smtp.SmtpMessage;
import org.junit.Before;
import org.junit.Ignore;
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
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, FakeSmtpMailServerFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.notification.core", "org.nuxeo.ecm.platform.notification.api",
        "org.nuxeo.ecm.platform.url.api", "org.nuxeo.ecm.platform.url.core" })
public class SendMailTest {

    protected DocumentModel src;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "File");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());
    }

    // ------ Tests comes here --------

    @Test
    @Ignore
    // disabling since it won't pass if the test RestTest.testSendMail is run
    // before, since this is setting the mail settings in a static final var
    public void testSendMail() throws Exception {
        // add some blobs and then send an email

        Blob blob = Blobs.createBlob("my content");
        blob.setFilename("thefile.txt");

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("sendEMail");
        chain.add(FetchContextDocument.ID);
        chain.add(SetDocumentBlob.ID).set("file", blob);
        chain.add(SendMail.ID).set("from", "test@nuxeo.org").set("to", "bs@nuxeo.com").set("subject", "test mail").set(
                "asHTML", true).set("files", "file:content").set(
                "message",
                "<h3>Current doc: ${Document.path}</h3> title: ${Document['dc:title']}<p>Doc link: <a href=\"${docUrl}\">${Document.title}</a>");
        service.run(ctx, chain);
        assertTrue(FakeSmtpMailServerFeature.server.getReceivedEmailSize() == 1);
        // Check that mandatory headers are present
        SmtpMessage message = (SmtpMessage) FakeSmtpMailServerFeature.server.getReceivedEmail().next();
        assertNotNull(message.getHeaderValue("Date"));
        assertEquals(message.getHeaderValue("From"), "test@nuxeo.org");
    }
}
