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

package org.nuxeo.ecm.platform.ec.notification.veto;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;

/**
 * For test only
 * @author tm@nuxeo.com
 *
 */
public class NotificationVeto1 implements NotificationListenerVeto {

    @Override
    public boolean accept(Event event) throws Exception {
        return false;
    }

}
