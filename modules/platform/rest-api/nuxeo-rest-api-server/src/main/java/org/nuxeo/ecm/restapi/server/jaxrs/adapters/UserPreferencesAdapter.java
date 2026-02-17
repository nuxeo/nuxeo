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
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import static javax.ws.rs.core.Response.Status.CREATED;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.user.preferences.api.UserPreference;
import org.nuxeo.user.preferences.api.UserPreferences;
import org.nuxeo.user.preferences.api.UserPreferencesService;
import org.nuxeo.user.preferences.exception.UserPreferencesNotFound;

/**
 * @since 2025.16
 */
@WebAdapter(name = UserPreferencesAdapter.NAME, type = "userPreferencesAdapter")
public class UserPreferencesAdapter extends DefaultAdapter {

    public static final String NAME = "preferences";

    protected CoreSession session;

    protected UserPreferencesService ups;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        ups = Framework.getService(UserPreferencesService.class);
        session = ctx.getCoreSession();
    }

    @DELETE
    public Response delete() {
        var docRef = getTarget().getAdapter(DocumentModel.class).getRef();
        ups.delete(session, docRef);
        return Response.noContent().build();
    }

    @GET
    public UserPreferences get() {
        var docRef = getTarget().getAdapter(DocumentModel.class).getRef();
        return ups.get(session, docRef);
    }

    @GET
    @Path("{key}")
    public UserPreference get(@PathParam("key") String key) {
        var docRef = getTarget().getAdapter(DocumentModel.class).getRef();
        return ups.get(session, docRef, key).orElseThrow(() -> new UserPreferencesNotFound(key));
    }

    @PUT
    public Response putAll(UserPreferences prefs) {
        var docRef = getTarget().getAdapter(DocumentModel.class).getRef();
        var response = ups.get(session, docRef).isEmpty()
                ? Response.status(CREATED).entity(ups.create(session, docRef, prefs.preferences()))
                : Response.ok(ups.putAll(session, docRef, prefs));
        return response.build();
    }

    @DELETE
    @Path("{key}")
    public UserPreferences remove(@PathParam("key") String key) {
        var docRef = getTarget().getAdapter(DocumentModel.class).getRef();
        return ups.remove(session, docRef, key);
    }

}
