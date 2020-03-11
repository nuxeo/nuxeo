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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract WebObject class that handle retrieve, deletion and update of {@link NuxeoPrincipal} or {@link NuxeoGroup}.
 *
 * @since 5.7.3
 */
public abstract class AbstractUMObject<T> extends DefaultObject {

    protected T currentArtifact;

    protected UserManager um;

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Object... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("UserObject takes at least one parameter");
        }
        um = Framework.getService(UserManager.class);
        currentArtifact = (T) args[0];
    }

    @GET
    public T doGetArtifact() {
        return currentArtifact;
    }

    @PUT
    public T doUpdateArtifact(T principal) {
        checkUpdateGuardPreconditions();
        return updateArtifact(principal);
    }

    @DELETE
    public Response doDeleteArtifact() {
        checkUpdateGuardPreconditions();
        deleteArtifact();
        return Response.status(Status.NO_CONTENT).build();
    }

    protected void checkUpdateGuardPreconditions() {
        NuxeoPrincipal principal = getContext().getCoreSession().getPrincipal();
        if (!principal.isAdministrator()) {
            if ((!principal.isMemberOf("powerusers")) || !isAPowerUserEditableArtifact()) {
                throw new WebSecurityException("User is not allowed to edit users");
            }
        }
    }

    /**
     * Check that the current artifact is editable by a power user. Basically this means not an admin user or not an
     * admin group.
     *
     * @return
     */
    protected abstract boolean isAPowerUserEditableArtifact();

    /**
     * Updates the current artifact by the one given in parameters in the underlying persistence system.
     *
     * @param artifact the artifact that has been retrieved from request.
     * @return the updated artifact.
     */
    protected abstract T updateArtifact(T artifact);

    /**
     * Deletes the current artifact in the underlying persistence system.
     */
    protected abstract void deleteArtifact();

}
