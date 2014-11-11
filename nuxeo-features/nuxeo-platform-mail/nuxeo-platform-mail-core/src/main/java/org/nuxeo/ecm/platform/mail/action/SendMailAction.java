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

package org.nuxeo.ecm.platform.mail.action;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Action to answer the mail. It expects the text of the answer to
 * be in the context under the "message" key.
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

    public boolean execute(ExecutionContext context) throws Exception {
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

    public void reset(ExecutionContext context) throws Exception {
        //do nothing
    }

}
