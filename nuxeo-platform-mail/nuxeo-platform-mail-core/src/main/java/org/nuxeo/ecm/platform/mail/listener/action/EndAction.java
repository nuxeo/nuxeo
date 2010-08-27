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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: EndAction.java 55400 2008-05-26 09:46:02Z atchertchian $
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAP;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.IMAPS;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.LEAVE_ON_SERVER_KEY;
import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.PROTOCOL_TYPE_KEY;

import javax.mail.Message;
import javax.mail.Flags.Flag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;

/**
 * @author Catalin Baican
 */
public class EndAction extends AbstractMailAction {

    private static final Log log = LogFactory.getLog(EndAction.class);

    @Override
    public boolean execute(ExecutionContext context) throws Exception {
        try {
            Message message = context.getMessage();
            // erase marker: mail has been treated
            // VDU it could be nice to have a field in schema 'protocol' that says
            // messages stay in server or not. This only work with IMAP* protocols, as POP3*
            // protocols does not support flags other than DELETED.
            //            message.setFlag(Flag.FLAGGED, false);
            boolean leaveOnServer = (Boolean) context.getInitialContext().get(LEAVE_ON_SERVER_KEY);
            String protocolType = (String) context.getInitialContext().get(PROTOCOL_TYPE_KEY);
//            log.debug(PROTOCOL_TYPE_KEY + ": " + protocolType);
//            log.debug(LEAVE_ON_SERVER_KEY + ": " + leaveOnServer);
            if ((IMAP.equals(protocolType) || IMAPS.equals(protocolType)) && leaveOnServer) {
                message.setFlag(Flag.SEEN, true);
            } else {
                message.setFlag(Flag.DELETED, true);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
