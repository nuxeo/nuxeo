/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.nuxeo.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 2023.4
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestMimeMessageHelper {

    @Test
    public void testMailMessageToMimeMessageConversion() throws MessagingException, IOException {
        var blob = Blobs.createBlob("def", "text/plain", "UTF-8", "abc");
        var mailMessage = new MailMessage.Builder("to@nx.com", "to-other@nx.com").from("from@nx.com")
                                                                                 .cc("cc@nx.com")
                                                                                 .bcc("bcc@nx.com")
                                                                                 .replyTo("reply@nx.com")
                                                                                 .attachments(blob)
                                                                                 .subject("test")
                                                                                 .content("some content")
                                                                                 .build();

        var mimeMessage = MimeMessageHelper.composeMimeMessage(mailMessage);

        var tos = mimeMessage.getRecipients(Message.RecipientType.TO);
        assertEquals(2, tos.length);
        assertEquals("to@nx.com", tos[0].toString());
        assertEquals("to-other@nx.com", tos[1].toString());
        assertEquals("from@nx.com", mimeMessage.getFrom()[0].toString());
        assertEquals("cc@nx.com", mimeMessage.getRecipients(Message.RecipientType.CC)[0].toString());
        assertEquals("bcc@nx.com", mimeMessage.getRecipients(Message.RecipientType.BCC)[0].toString());
        assertEquals("reply@nx.com", mimeMessage.getReplyTo()[0].toString());
        assertEquals("test", mimeMessage.getSubject());
        assertTrue(mimeMessage.getDataHandler().getContentType().startsWith("multipart/mixed"));

        var multipart = (MimeMultipart) mimeMessage.getContent();
        assertEquals(2, multipart.getCount());

        BodyPart body = multipart.getBodyPart(0);
        assertEquals("text/plain; charset=utf-8", body.getDataHandler().getContentType());
        assertEquals("some content", body.getContent());

        BodyPart attachment = multipart.getBodyPart(1);
        var dataSource = (BlobDataSource) attachment.getDataHandler().getDataSource();
        var stringBlob = dataSource.blob();
        assertEquals("def", stringBlob.getString());
        assertEquals("text/plain", stringBlob.getMimeType());
        assertEquals("UTF-8", stringBlob.getEncoding());
        assertEquals("abc", stringBlob.getFilename());
    }
}
