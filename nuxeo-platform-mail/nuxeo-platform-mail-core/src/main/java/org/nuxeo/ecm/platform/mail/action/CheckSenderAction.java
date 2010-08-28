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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Action that check the mail address against the user directory.
 * If the address of the sender is not in the user directory, the
 * mail is not processed further.
 * <p>
 * If the sender is in the user directory, it put the principal as a
 * string in the context under the "sender" key.
 *
 * @author Alexandre Russel
 */
public class CheckSenderAction implements MessageAction {

    private static final Log log = LogFactory.getLog(CheckSenderAction.class);

    public boolean execute(ExecutionContext context) throws Exception {
        Message message = context.getMessage();
        Address[] addresses = message.getFrom();
        if (addresses == null || addresses.length == 0 || !(addresses[0] instanceof InternetAddress)) {
            log.debug("No internet messages, stopping the pipe: " + message);
            return false;
        }
        InternetAddress address = (InternetAddress) addresses[0];
        String principal = getPrincipal(address.getAddress());
        if (principal == null) {
            log.debug("Sender not in user directory. Stop processing");
            return false;
        }
        context.put("sender", principal);
        return true;
    }

    private static String getPrincipal(String address) throws Exception {
        Session session = null;
        String principal = null;
        try {
            DirectoryService directoryService = Framework.getService(DirectoryService.class);
            session = directoryService.open("userDirectory");
            Map<String, Serializable> map = new HashMap<String, Serializable>();
            map.put("email", address);
            DocumentModelList list = session.query(map);
            if (list == null || list.isEmpty()) {
                log.debug("Stopping pipe, address: " + address + " return " + list);
                return null;
            }
            DocumentModel dm = list.get(0);
            principal = dm.getId();
        } finally {
            session.close();
        }
        return principal;
    }

    public void reset(ExecutionContext context) throws Exception {
        //do nothing
    }

}
