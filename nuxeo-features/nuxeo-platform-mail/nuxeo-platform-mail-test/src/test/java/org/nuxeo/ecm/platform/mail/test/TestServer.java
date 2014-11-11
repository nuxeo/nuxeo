/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.test;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.Flags.Flag;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

/**
 * @author Alexandre Russel
 */
public class TestServer extends TestCase {

    private static final String FACINATING_CONTENT = "facinating content.";
    private static final String TEST_EMAIL_SERVER = "Test email server";

    private Message message;
    private final Properties props = new Properties();

    @Override
    protected void setUp() throws Exception {
        Server.start();
        props.put("mail.store.protocol", "pop3");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.port", "60025");
        props.put("mail.host", "localhost");
        props.put("mail.user", "alex@localhost");
        props.put("mail.from", "alex@localhost");
        props.put("mail.pop3.port", "60110");
        Session session = Session.getDefaultInstance(props);
        message = new MimeMessage(session);
        message.setSubject(TEST_EMAIL_SERVER);
        Address address = new InternetAddress("alex@localhost");
        message.setRecipient(Message.RecipientType.TO, address);
        message.setFrom();
        message.setText(FACINATING_CONTENT);
        Transport.send(message);
    }

    @Override
    protected void tearDown() throws Exception {
        Server.shutdown();
    }

    public void testServer() throws Exception {
        Session session = Session.getDefaultInstance(props);
        Thread.sleep(1000);
        Store store = session.getStore();
        store.connect("alex@localhost", "mdpalex");
        Folder rootFolder = store.getFolder("INBOX");
        int count = 0;
        while (count != 1) {
            rootFolder.open(Folder.READ_ONLY);
            count = rootFolder.getMessageCount();
            rootFolder.close(false);
        }
        rootFolder.open(Folder.READ_WRITE);
        Message[] messages = rootFolder.getMessages();
        assertEquals(1, messages.length);
        Message message = messages[0];
        assertEquals(TEST_EMAIL_SERVER, message.getSubject());
        message.setFlag(Flag.DELETED, true);
        MimeMessage mm = (MimeMessage) message;
        String contentType = mm.getContentType();
        assertEquals("text/plain; charset=us-ascii", contentType);
        String content = (String) mm.getContent();
        // a \n is appended to the content
        assertTrue(content.startsWith(FACINATING_CONTENT));
        rootFolder.close(true);
    }

}
