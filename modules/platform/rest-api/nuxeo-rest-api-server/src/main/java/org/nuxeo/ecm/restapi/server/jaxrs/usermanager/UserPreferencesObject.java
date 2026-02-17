/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.user.preferences.api.UserPreference;
import org.nuxeo.user.preferences.api.UserPreferencesService;
import org.nuxeo.user.preferences.exception.UserPreferencesNotFound;

/**
 * @since 2025.16
 */
@WebObject(type = "preferences")
@Produces(APPLICATION_JSON)
public class UserPreferencesObject extends DefaultObject {

    protected String key;

    protected CoreSession session;

    protected UserPreferencesService ups;

    @Override
    protected void initialize(Object... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("UserPreferencesObject takes preference key as parameter");
        }
        key = (String) args[0];
        ups = Framework.getService(UserPreferencesService.class);
        session = ctx.getCoreSession();
    }

    @DELETE
    public Response delete() {
        ups.delete(session, key);
        return Response.noContent().build();
    }

    @GET
    public UserPreference get() {
        return ups.get(session, key).orElseThrow(() -> new UserPreferencesNotFound(key));
    }

    @PUT
    public Response createOrUpdate(String value) {
        return ups.get(session, key)
                  .map(pref -> Response.ok(ups.update(session, key, value)))
                  .orElseGet(() -> Response.status(CREATED).entity(ups.create(session, key, value)))
                  .build();
    }

}
