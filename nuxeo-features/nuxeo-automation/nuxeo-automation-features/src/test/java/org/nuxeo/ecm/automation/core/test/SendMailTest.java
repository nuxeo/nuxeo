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
package org.nuxeo.ecm.automation.core.test;

import java.io.File;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.platform.notification.core",
        "org.nuxeo.ecm.platform.notification.api",
        "org.nuxeo.ecm.platform.url.api",
        "org.nuxeo.ecm.platform.url.core" })
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

        InputStream in = SendMailTest.class.getClassLoader().getResourceAsStream(
                "example.mail.properties");
        File file = new File(Environment.getDefault().getConfig(),
                "mail.properties");
        file.getParentFile().mkdirs();
        FileUtils.copyToFile(in, file);
        in.close();

    }

    // ------ Tests comes here --------

    @Ignore("This test is disabled since the mail configuration may not work correctly on hudson. and anyway we need to check the received mail format.")
    @Test
    public void testSendMail() throws Exception {
        // add some blobs and then send an email

        StringBlob blob = new StringBlob("my content");
        blob.setMimeType("text/plain");
        blob.setEncoding("UTF-8");
        blob.setFilename("thefile.txt");

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("sendEMail");
        chain.add(FetchContextDocument.ID);
        chain.add(SetDocumentBlob.ID).set("file", blob);
        chain.add(SendMail.ID).set("from", "test@nuxeo.org").set("to",
                "bs@nuxeo.com").set("subject", "test mail")
                .set("asHTML", true)
                .set("files", "file:content")
                .set(
                "message",
                "<h3>Current doc: ${Document.path}</h3> title: ${Document['dc:title']}<p>Doc link: <a href=\"${docUrl}\">${Document.title}</a>");
        service.run(ctx, chain);

    }

}
