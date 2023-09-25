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

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/**
 * Default implementation of {@link MailSender} building {@link MimeMessage}s and sending via SMTP protocol.
 *
 * @since 2023.4
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
            Transport.send(MimeMessageHelper.composeMimeMessage(msg, session));
        } catch (MessagingException e) {
            throw new MailException("An error occurred while sending a mail", e);
        }
    }

}
