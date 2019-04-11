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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Action that check the mail address against the user directory. If the address of the sender is not in the user
 * directory, the mail is not processed further.
 * <p>
 * If the sender is in the user directory, it put the principal as a string in the context under the "sender" key.
 *
 * @author Alexandre Russel
 */
public class CheckSenderAction implements MessageAction {

    private static final Log log = LogFactory.getLog(CheckSenderAction.class);

    @Override
    public boolean execute(ExecutionContext context) throws MessagingException {
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

    private static String getPrincipal(String address) {
        String principal;
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        try (Session session = directoryService.open("userDirectory")) {

            Map<String, Serializable> map = new HashMap<>();
            map.put("email", address);
            DocumentModelList list = session.query(map);
            if (list == null || list.isEmpty()) {
                log.debug("Stopping pipe, address: " + address + " return " + list);
                return null;
            }
            DocumentModel dm = list.get(0);
            principal = dm.getId();
        }
        return principal;
    }

    @Override
    public void reset(ExecutionContext context) {
        // do nothing
    }

}
