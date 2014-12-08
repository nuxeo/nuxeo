/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.user.registration.webengine;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.ecm.webengine.invite.UserInvitationObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

@Path("/userRegistration")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "userRegistration")
public class UserRegistrationObject extends UserInvitationObject {

    @Override
    protected UserInvitationService fetchService() {
        return Framework.getLocalService(UserRegistrationService.class);
    }

}
