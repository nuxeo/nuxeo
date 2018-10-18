/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 *
 * $Id:
 */

package org.nuxeo.ecm.platform.mail.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PARENT_PATH_KEY;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.action.Visitor;
import org.nuxeo.ecm.platform.mail.utils.MailCoreConstants;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Laurent Doguin
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.mail")
@Deploy("org.nuxeo.ecm.platform.mail.types")
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.convert.api")
@Deploy("org.nuxeo.ecm.core.convert.plugins")
public class TestMailInjection {

    @Inject
    protected MailService mailService;

    @Inject
    protected CoreSession session;

    protected DocumentModel mailFolder1;

    protected DocumentModel mailFolder2;

    @Before
    public void setUp() {
        createMailFolders();
    }

    @Test
    public void testMailUnicityCheck() throws Exception {
        assertNotNull(session.getDocument(new PathRef("/mailFolder1")));
        assertNotNull(session.getDocument(new PathRef("/mailFolder2")));
        injectEmail("data/test_mail.eml", mailFolder1.getPathAsString());
        DocumentModelList children = session.getChildren(mailFolder1.getRef());
        assertNotNull(children);
        assertTrue(!children.isEmpty());
        assertEquals(1, children.size());
        injectEmail("data/test_mail2.eml", mailFolder1.getPathAsString());
        children = session.getChildren(mailFolder1.getRef());
        assertEquals(2, children.size());
        injectEmail("data/test_mail.eml", mailFolder1.getPathAsString());
        children = session.getChildren(mailFolder1.getRef());
        // size won't change because same mail has been injected
        assertEquals(2, children.size());
        // inject previously injected mail in another mail folder
        injectEmail("data/test_mail.eml", mailFolder2.getPathAsString());
        injectEmail("data/test_mail2.eml", mailFolder2.getPathAsString());
        children = session.getChildren(mailFolder2.getRef());
        assertNotNull(children);
        assertTrue(!children.isEmpty());
        assertEquals(2, children.size());
    }

    private void injectEmail(String filePath, String parentPath) throws Exception {
        MessageActionPipe pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        Visitor visitor = new Visitor(pipe);
        ExecutionContext initialExecutionContext = new ExecutionContext();
        assertNotNull(session.getSessionId());
        initialExecutionContext.put(MailCoreConstants.CORE_SESSION_KEY, session);
        initialExecutionContext.put(MailCoreConstants.MIMETYPE_SERVICE_KEY,
                Framework.getService(MimetypeRegistry.class));
        initialExecutionContext.put(PARENT_PATH_KEY, parentPath);

        Message[] messages = { getSampleMessage(filePath) };

        visitor.visit(messages, initialExecutionContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBadMailAddress() throws Exception {
        injectEmail("data/test_bad_mail_address.eml", mailFolder1.getPathAsString());
        DocumentModelList children = session.getChildren(mailFolder1.getRef());
        assertNotNull(children);
        assertTrue(!children.isEmpty());
        assertEquals(1, children.size());
        DocumentModel mail = children.get(0);
        List<String> recipients = (List<String>) mail.getPropertyValue("mail:recipients");
        assertNotNull(recipients);
        assertTrue(!recipients.isEmpty());
        assertEquals(recipients.get(0), "<Undisclosed-Recipient:;>");
        List<String> ccRecipients = (List<String>) mail.getPropertyValue("mail:cc_recipients");
        assertNotNull(ccRecipients);
        assertTrue(!ccRecipients.isEmpty());
        assertEquals(ccRecipients.get(0), "<Undisclosed-Recipient:;>");
        assertEquals(mail.getProperty("mail:sender").getValue(String.class), "Nicolas Ulrich <nulrich:nuxeo.com>");
    }

    private Message getSampleMessage(String filePath) throws Exception {
        InputStream stream = new FileInputStream(getTestMailSource(filePath));
        return new MimeMessage(null, stream);
    }

    private String getTestMailSource(String filePath) {
        return FileUtils.getResourcePathFromContext(filePath);
    }

    private void createMailFolders() {
        mailFolder1 = session.createDocumentModel("/", "mailFolder1", MailCoreConstants.MAIL_FOLDER_TYPE);
        session.createDocument(mailFolder1);
        session.saveDocument(mailFolder1);
        mailFolder2 = session.createDocumentModel("/", "mailFolder2", MailCoreConstants.MAIL_FOLDER_TYPE);
        session.createDocument(mailFolder2);
        session.saveDocument(mailFolder2);
        session.save();
    }

}
