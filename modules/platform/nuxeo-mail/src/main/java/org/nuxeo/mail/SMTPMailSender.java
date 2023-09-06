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
 */

package org.nuxeo.mail;

import java.util.List;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Default implementation of MailSender building {@link MimeMessage}s and sending via SMTP protocol.
 *
 * @since 2023.3
 */
public class SMTPMailSender implements MailSender {

    protected final Session session;

    public SMTPMailSender(MailSenderDescriptor descriptor) {
        this(MailSessionBuilder.fromProperties(descriptor.properties).build());
    }

    protected SMTPMailSender(Session session) {
        this.session = session;
    }

    @Override
    public void sendMail(MailMessage msg) {
        try {
            MimeMessage mimeMsg = composeMimeMessage(msg);

            mimeMsg.addFrom(toAddresses(msg.getFroms()));
            mimeMsg.setRecipients(MimeMessage.RecipientType.TO, toAddresses(msg.getTos()));
            mimeMsg.setRecipients(MimeMessage.RecipientType.CC, toAddresses(msg.getCcs()));
            mimeMsg.setRecipients(MimeMessage.RecipientType.BCC, toAddresses(msg.getBccs()));
            mimeMsg.setReplyTo(toAddresses(msg.getReplyTos()));
            mimeMsg.setSentDate(msg.getDate());
            mimeMsg.setSubject(msg.getSubject(), msg.getSubjectCharset().toString());
            Transport.send(mimeMsg);
        } catch (MessagingException e) {
            throw new NuxeoException("An error occurred while sending a mail", e);
        }
    }

    protected MimeMessage composeMimeMessage(MailMessage msg) throws MessagingException {
        var mail = new MimeMessage(session);
        if (!msg.hasAttachments()) {
            mail.setContent(msg.getContent(), msg.getContentType());
        } else { // text goes into a body part
            var body = new MimeBodyPart();
            body.setContent(msg.getContent(), msg.getContentType());
            // then get the attachments
            MimeMultipart bodyParts = assembleMultiPart(body, msg.getAttachments());
            mail.setContent(bodyParts);
        }
        return mail;
    }

    protected MimeMultipart assembleMultiPart(MimeBodyPart body, List<Blob> attachments) throws MessagingException {
        var mp = new MimeMultipart();
        mp.addBodyPart(body);
        for (Blob blob : attachments) {
            MimeBodyPart a = new MimeBodyPart();
            a.setDataHandler(new DataHandler(new BlobDataSource(blob)));
            a.setFileName(blob.getFilename());
            mp.addBodyPart(a);
        }
        return mp;
    }

    protected Address[] toAddresses(List<String> list) {
        return list.stream().map(a -> {
            try {
                return new InternetAddress(a);
            } catch (AddressException e) {
                throw new NuxeoException("Could not parse mail address: " + a, e);
            }
        }).toArray(InternetAddress[]::new);
    }
}
