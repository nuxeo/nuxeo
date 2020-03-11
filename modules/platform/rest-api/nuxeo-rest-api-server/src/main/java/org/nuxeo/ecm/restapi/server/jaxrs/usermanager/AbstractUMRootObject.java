/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.restapi.server.jaxrs.PaginableObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractUMRootObject<T> extends PaginableObject<T> {

    protected String query;

    protected UserManager um;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        um = Framework.getService(UserManager.class);

        final HttpServletRequest request = ctx.getRequest();
        query = request.getParameter("q");
    }

    // match everything until:
    // - '/@' for web adapters
    // - '/user/' or '/group/' when adding a user to a group
    @Path("{artName:((?:(?!(/@|(/user/|/group/))).)*)}")
    public Object getArtifactWebObject(@PathParam("artName") String artName) {
        T artifact = getArtifact(artName);
        if (artifact == null) {
            throw new WebResourceNotFoundException(getArtifactType() + " does not exist");
        }
        return newObject(getArtifactType(), artifact);
    }

    @POST
    public Response createNew(T artifact) {
        checkPrecondition(artifact);
        artifact = createArtifact(artifact);
        return Response.status(Status.CREATED).entity(artifact).build();
    }

    @GET
    @Path("search")
    public List<T> search() {
        return getPaginableEntries();
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { query };
    }

    /**
     * Returns the artifact given its id.
     */
    protected abstract T getArtifact(String id);

    /**
     * Returns the type of the current artifact needed for {@link #newObject(String, Object...)}.
     */
    protected abstract String getArtifactType();

    /**
     * Checks the precondition to create an artifact (for instance validity, duplicate detection, guards...).
     */
    protected abstract void checkPrecondition(T artifact);

    /**
     * Persist an artifact in the underlying persistence system.
     */
    protected abstract T createArtifact(T artifact);

    protected void checkCurrentUserCanCreateArtifact(T artifact) {
        NuxeoPrincipal currentUser = getContext().getCoreSession().getPrincipal();
        if (!currentUser.isAdministrator()) {
            if (!currentUser.isMemberOf("powerusers") || !isAPowerUserEditableArtifact(artifact)) {
                throw new WebSecurityException("Cannot create artifact");
            }
        }
    }

    abstract boolean isAPowerUserEditableArtifact(T artifact);

}
