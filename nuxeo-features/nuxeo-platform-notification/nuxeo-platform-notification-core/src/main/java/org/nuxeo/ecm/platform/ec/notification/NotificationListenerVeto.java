/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ec.notification;

import org.nuxeo.ecm.core.event.Event;

/**
 * @description ensure the event is valid to send notifications
 * @since 5.6
 * @author Thierry Martins <tm@nuxeo.com>
 *
 */
public interface NotificationListenerVeto {

    boolean accept(Event event) throws Exception;

}
