/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.registration;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.user.invite.RegistrationRules;
import org.nuxeo.ecm.user.invite.UserRegistrationConfiguration;
import org.nuxeo.runtime.api.Framework;

public class UserRegistrationListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        UserRegistrationService userRegistrationService = Framework.getService(UserRegistrationService.class);
        if (!event.getName().equals(userRegistrationService.getNameEventRegistrationValidated())) {
            return;
        }
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel registration = docCtx.getSourceDocument();
            UserRegistrationConfiguration config = userRegistrationService.getConfiguration(registration);
            RegistrationRules rules = userRegistrationService.getRegistrationRules(config.getName());
            if (rules.allowUserCreation()) {
                NuxeoPrincipal principal = userRegistrationService.createUser(ctx.getCoreSession(), registration);
                docCtx.setProperty("registeredUser", principal);
            }
            if (rules.allowUserCreation() || rules.isForcingRight()) {
                userRegistrationService.addRightsOnDoc(ctx.getCoreSession(), registration);
            }
        }
    }

}
