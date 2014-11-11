/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.usermanager;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract WebObject class that handle retrieve, deletion and update of
 * {@link NuxeoPrincipal} or {@link NuxeoGroup}.
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
            throw new IllegalArgumentException(
                    "UserObject takes at least one parameter");
        }
        um = Framework.getLocalService(UserManager.class);
        currentArtifact = (T) args[0];
    }

    @GET
    public T doGetArtifact() {
        return currentArtifact;
    }

    @PUT
    public T doUpdateArtifact(T principal) {
        try {
            checkUpdateGuardPreconditions();
            return updateArtifact(principal);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @DELETE
    public Response doDeleteArtifact() {
        try {
            checkUpdateGuardPreconditions();
            deleteArtifact();
            return Response.status(Status.NO_CONTENT).build();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    protected void checkUpdateGuardPreconditions() throws ClientException {
        NuxeoPrincipal principal = (NuxeoPrincipal) getContext().getCoreSession().getPrincipal();
        if (!principal.isAdministrator()) {
            if ((!principal.isMemberOf("powerusers"))
                    || !isAPowerUserEditableArtifact()) {

                throw new WebSecurityException(
                        "User is not allowed to edit users");
            }
        }
    }

    /**
     * Check that the current artifact is editable by a power user. Basically
     * this means not an admin user or not an admin group.
     *
     * @return
     *
     */
    protected abstract boolean isAPowerUserEditableArtifact();

    /**
     * Updates the current artifact by the one given in parameters in the
     * underlying persistence system.
     *
     * @param artifact the artifact that has been retrieved from request.
     * @return the updated artifact.
     * @throws ClientException
     *
     */
    protected abstract T updateArtifact(T artifact) throws ClientException;

    /**
     * Deletes the current artifact in the underlying persistence system.
     *
     * @throws ClientException
     *
     */
    protected abstract void deleteArtifact() throws ClientException;

}
