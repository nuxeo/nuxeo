/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.ec.notification;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Veto not to send notification when a version is the source document of the event. Notifications on versions are not
 * really relevant, there should be another event on the live document that provides a more specific notification For
 * instance, use documentCheckedIn on the live document, instead of documentCreated on the version
 *
 * @since 5.7
 * @author Thierry Martins <tm@nuxeo.com>
 */
public class VersionVeto implements NotificationListenerVeto {

    @Override
    public boolean accept(Event event) {
        // this cast is safe because the type checking was done in
        // NotificationEventListener
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        if (docCtx.getSourceDocument().isVersion()) {
            return false;
        }
        return true;
    }

}
