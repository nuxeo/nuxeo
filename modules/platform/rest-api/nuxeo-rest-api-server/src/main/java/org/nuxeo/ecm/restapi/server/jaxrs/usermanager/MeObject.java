/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 9.1
 */
@WebObject(type = "me")
@Produces({ MediaType.APPLICATION_JSON })
public class MeObject extends DefaultObject {

    @GET
    public NuxeoPrincipal doGet(@Context Request request) {
        return getContext().getCoreSession().getPrincipal();
    }

    @PUT
    @Path("changepassword")
    public Object changePassword(String payload) throws JSONException {
        NuxeoPrincipal currentUser = getContext().getCoreSession().getPrincipal();
        JSONObject payloadJson = new JSONObject(payload);
        String oldPassword = payloadJson.getString("oldPassword");
        String newPassword = payloadJson.getString("newPassword");
        UserManager userManager = Framework.getService(UserManager.class);
        if (userManager.checkUsernamePassword(currentUser.getName(), oldPassword)) {
            currentUser.setPassword(newPassword);
            Framework.doPrivileged(() -> userManager.updateUser(currentUser.getModel()));
            return currentUser;
        } else {
            return Response.status(Status.UNAUTHORIZED).build();
        }

    }

}
