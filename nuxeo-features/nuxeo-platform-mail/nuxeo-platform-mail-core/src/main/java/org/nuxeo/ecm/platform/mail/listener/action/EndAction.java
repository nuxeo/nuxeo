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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: EndAction.java 55400 2008-05-26 09:46:02Z atchertchian $
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAP;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAPS;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.LEAVE_ON_SERVER_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PROTOCOL_TYPE_KEY;

import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.nuxeo.ecm.platform.mail.action.ExecutionContext;

/**
 * @author Catalin Baican
 */
public class EndAction extends AbstractMailAction {

    @Override
    public boolean execute(ExecutionContext context) {
        try {
            Message message = context.getMessage();
            // erase marker: mail has been treated
            // VDU it could be nice to have a field in schema 'protocol' that says
            // messages stay in server or not. This only work with IMAP* protocols, as POP3*
            // protocols does not support flags other than DELETED.
            // message.setFlag(Flag.FLAGGED, false);
            boolean leaveOnServer = (Boolean) context.getInitialContext().get(LEAVE_ON_SERVER_KEY);
            String protocolType = (String) context.getInitialContext().get(PROTOCOL_TYPE_KEY);
            // log.debug(PROTOCOL_TYPE_KEY + ": " + protocolType);
            // log.debug(LEAVE_ON_SERVER_KEY + ": " + leaveOnServer);
            if ((IMAP.equals(protocolType) || IMAPS.equals(protocolType)) && leaveOnServer) {
                message.setFlag(Flag.SEEN, true);

            } else {
                message.setFlag(Flag.DELETED, true);
            }
            return true;
        } catch (MessagingException e) {
            return false;
        }
    }

}
