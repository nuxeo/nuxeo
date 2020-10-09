/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Action to answer the mail. It expects the text of the answer to be in the context under the "message" key.
 *
 * @author Alexandre Russel
 */
public class SendMailAction implements MessageAction {

    private static final Log log = LogFactory.getLog(SendMailAction.class);

    protected final Session session;

    protected final String textMessage;

    public SendMailAction(Session session, String textMessage) {
        this.session = session;
        this.textMessage = textMessage;
    }

    @Override
    public boolean execute(ExecutionContext context) throws MessagingException {
        Message message = context.getMessage();
        if (log.isDebugEnabled()) {
            log.debug("Sending mail because of message: " + message.getSubject());
        }
        Message sentMessage = new MimeMessage(session);
        if (message.getReplyTo() == null || message.getReplyTo().length == 0) {
            return true;
        }
        Address address = message.getReplyTo()[0];
        sentMessage.setRecipient(Message.RecipientType.TO, address);
        message.setText(textMessage);
        Transport.send(sentMessage);
        return true;
    }

    @Override
    public void reset(ExecutionContext context) {
        // do nothing
    }

}
