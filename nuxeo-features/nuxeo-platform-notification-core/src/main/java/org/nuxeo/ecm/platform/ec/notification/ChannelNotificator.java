/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     narcis
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/**
 * Interface that should be implemented by classes handling the sending
 * of messages according to the notifications registered in the system.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
// FIXME: this interface is never implemented.
public interface ChannelNotificator {
    /**
     * Sends the notification (email, sms, jabber, etc.).
     *
     * @param docMessage
     * @throws ClientException
     * @throws Exception
     */
    void sendNotification(DocumentMessage docMessage) throws Exception;

    /**
     * Checks if the cannel is the right one (email, sms, etc.).
     * By default only email is inplemented.
     *
     * @param docMessage
     * @return
     */
    boolean isInterestedInNotification(NotificationImpl docMessage);

}
