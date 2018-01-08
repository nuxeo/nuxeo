/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 */
package org.nuxeo.ecm.platform.mail.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MailBoxActions;
import org.nuxeo.ecm.platform.mail.action.MessageAction;
import org.nuxeo.ecm.platform.mail.action.MessageActionPipe;
import org.nuxeo.ecm.platform.mail.test.Server;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * The test of this class is commented out because it should not be run in CI environement. To test the mail service
 * remove the _ infront of the testTest method.
 *
 * @author Alexandre Russel
 */
public class TestMailService extends NXRuntimeTestCase {
    private static final String VERY_SMALL_MAIL = "very small mail";

    private static final String A_DEAD_SIMPLE_MAIL = "a dead simple mail";

    private static final String A_GREAT_TEMPLATE_MAIL = "A great template mail";

    private static final String TEST_FACTORY = "testFactory";

    private InternetAddress internetAddress;

    MailService mailService;

    @Override
    public void setUp() throws Exception {
        deployBundle("org.nuxeo.ecm.webapp.base");
        // deployBundle("org.nuxeo.ecm.platform.mail");
        // deployBundle("org.nuxeo.ecm.platform.mail.test");
        // mailService = Framework.getService(MailService.class);
        // internetAddress = new InternetAddress("alex@localhost");

    }

    @Override
    public void tearDown() throws Exception {
        Server.shutdown();
    }

    @Test
    public void testTrueTest() {
        assertTrue(true);
    }

    public void _testTest() throws Exception {
        assertNotNull(mailService);
        Session session = mailService.getSession(TEST_FACTORY);
        assertNotNull(session);
        Message message = new MimeMessage(session);
        assertNotNull(message);
        message.setFrom();
        message.setSubject("foo");
        message.setText("Ye Ye Ye.");
        message.setRecipient(Message.RecipientType.TO, internetAddress);
        Transport transport = mailService.getConnectedTransport(TEST_FACTORY);
        Transport.send(message);
        assertNotNull(transport);
        transport.close();
        Store store = mailService.getConnectedStore(TEST_FACTORY);
        assertNotNull(store);
        Folder rootFolder = store.getFolder("INBOX");
        assertNotNull(rootFolder);
        int count = 0;
        int maxIteration = 100000; // not get into infinity if doing CI.
        while (count != 1 && maxIteration-- > 0) {
            rootFolder.open(Folder.READ_ONLY);
            count = rootFolder.getMessageCount();
            rootFolder.close(false);
        }
        rootFolder.open(Folder.READ_ONLY);
        Message[] messages = rootFolder.getMessages();
        assertEquals(1, messages.length);
        message = messages[0];
        message.setFlag(Flag.DELETED, true);
        assertEquals("foo", message.getSubject());
        rootFolder.close(true);
        store.close();
        Map<String, Object> variablesMap = new HashMap<>();
        variablesMap.put("sender", "alex");
        mailService.sendMail("templates/SimpleMail.tpl", A_GREAT_TEMPLATE_MAIL, TEST_FACTORY,
                new Address[] { internetAddress });
        mailService.sendMail(VERY_SMALL_MAIL, A_DEAD_SIMPLE_MAIL, TEST_FACTORY, new Address[] { internetAddress });
        MailBoxActions mba = mailService.getMailBoxActions(TEST_FACTORY, "INBOX");
        assertNotNull(mba);
        mba.addAction(new TestMailAction());
        mba.execute();
        assertEquals(1, TestMailAction.counter);
        store = mailService.getConnectedStore(TEST_FACTORY);
        // doing some clean up before leaving
        rootFolder = store.getFolder("INBOX");
        count = 0;
        maxIteration = 100; // not get into infinity if doing CI.
        while (count != 2 && maxIteration-- > 0) {
            rootFolder.open(Folder.READ_ONLY);
            count = rootFolder.getMessageCount();
            rootFolder.close(false);
            Thread.sleep(2000);
        }
        rootFolder.open(Folder.READ_ONLY);
        messages = rootFolder.getMessages();
        for (Message m : messages) {
            MimeMessage mimeM = (MimeMessage) m;
            String content = (String) mimeM.getContent();
            if (m.getSubject().equals(A_GREAT_TEMPLATE_MAIL)) {
                assertTrue(content.contains("I am a simple mail sent by alex."));
            } else if (m.getSubject().equals(A_DEAD_SIMPLE_MAIL)) {
                assertTrue(content.contains(VERY_SMALL_MAIL));
            }
            m.setFlag(Flag.DELETED, true);
        }
        rootFolder.close(true);
        store.close();
    }

    @Test
    public void testServiceRegistration() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.mail");
        applyInlineDeployments();

        MailService mailService = Framework.getService(MailService.class);
        assertNotNull(mailService);
        MessageActionPipe pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        assertEquals(4, pipe.size());
        assertEquals(pipe.get(0).getClass().getSimpleName(), "StartAction");
        assertEquals(pipe.get(1).getClass().getSimpleName(), "ExtractMessageInformationAction");
        assertEquals(pipe.get(2).getClass().getSimpleName(), "CheckMailUnicity");
        assertEquals(pipe.get(3).getClass().getSimpleName(), "CreateDocumentsAction");
        // assertEquals(pipe.get(4).getClass().getSimpleName(), "EndAction");
        // test contribution merge
        pushInlineDeployments("org.nuxeo.ecm.platform.mail.test:OSGI-INF/mailService-test-contrib.xml");
        mailService = Framework.getService(MailService.class);

        pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        assertEquals(4, pipe.size());
        assertEquals(pipe.get(0).getClass().getSimpleName(), "StartAction");
        assertEquals(pipe.get(1).getClass().getSimpleName(), "ExtractMessageInformationAction");
        assertEquals(pipe.get(2).getClass().getSimpleName(), "CreateDocumentsAction");
        assertEquals(pipe.get(3).getClass().getSimpleName(), "CreateDocumentsAction");
        // assertEquals(pipe.get(4).getClass().getSimpleName(), "EndAction");
        // test contribution override
        pushInlineDeployments("org.nuxeo.ecm.platform.mail.test:OSGI-INF/mailService-override-test-contrib.xml");
        mailService = Framework.getService(MailService.class);

        pipe = mailService.getPipe("nxmail");
        assertNotNull(pipe);
        assertEquals(2, pipe.size());
        assertEquals(pipe.get(0).getClass().getSimpleName(), "ExtractMessageInformationAction");
        assertEquals(pipe.get(1).getClass().getSimpleName(), "CreateDocumentsAction");

    }

    @Test
    public void testSessionFactoryUnique() throws Exception {
        deployBundle("org.nuxeo.ecm.platform.mail");
        pushInlineDeployments("org.nuxeo.ecm.platform.mail.test:OSGI-INF/mailService-session-factory-test-contrib.xml");

        MailService mailService = Framework.getService(MailService.class);
        Session session1 = mailService.getSession("testFactory");
        assertNotNull(session1);
        // check we get the same session by getting a session again
        Session session1a = mailService.getSession("testFactory");
        assertNotNull(session1a);
        // check equality by reference
        assertSame("Sessions should be equals", session1,  session1a);

        // now get a new session
        Session session2 = mailService.getSession("testFactory2");
        assertNotNull(session2);
        assertNotSame("Sessions shouldn't be equals", session1, session2);
    }

    static class TestMailAction implements MessageAction {
        private static int counter;

        @Override
        public boolean execute(ExecutionContext context) {
            counter++;
            return true;
        }

        @Override
        public void reset(ExecutionContext context) {
        }
    }

}
