/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
        return Framework.getService(UserRegistrationService.class);
    }

}
