/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
