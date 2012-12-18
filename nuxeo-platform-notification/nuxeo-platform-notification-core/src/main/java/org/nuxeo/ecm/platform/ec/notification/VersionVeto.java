/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.ec.notification;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Veto not to send notification when a version is the source document of the
 * event. Notifications on versions are not really relevant, there should be
 * another event on the live document that provides a more specific notification
 * For instance, use documentCheckedIn on the live document, instead of
 * documentCreated on the version
 *
 * @since 5.7
 * @author Thierry Martins <tm@nuxeo.com>
 *
 */
public class VersionVeto implements NotificationListenerVeto {

    @Override
    public boolean accept(Event event) throws Exception {
        // this cast is safe because the type checking was done in
        // NotificationEventListener
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        if (docCtx.getSourceDocument().isVersion()) {
            return false;
        }
        return true;
    }

}
