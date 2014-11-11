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
package org.nuxeo.ecm.automation.server.jaxrs.usermanager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
public abstract class AbstractUMRootObject<T> extends DefaultObject {

    protected UserManager um;

    @Override
    protected void initialize(Object... args) {
        um = Framework.getLocalService(UserManager.class);
    }

    @Path("{artName}")
    public Object getArtifactWebObject(@PathParam("artName")
    String artName) {
        try {
            T artifact = getArtifact(artName);
            if (artifact == null) {
                throw new WebResourceNotFoundException(getArtifactType()
                        + " does not exist");
            }
            return newObject(getArtifactType(), artifact);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    public Response createNew(T artifact) {
        try {
            checkPrecondition(artifact);
            artifact = createArtifact(artifact);
            return Response.status(Status.CREATED).entity(artifact).build();

        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("search")
    public Response search(final @QueryParam("q")
    String query) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, InvocationTargetException {
        Method method = this.getClass().getDeclaredMethod("searchArtifact",
                String.class);

        GenericEntity<Object> entity = new GenericEntity<Object>(method.invoke(
                this, query), method.getGenericReturnType());
        return Response.ok(entity).build();

    }

    /**
     * @param query
     * @return
     *
     */
    protected abstract List<T> searchArtifact(String query)
            throws ClientException;

    /**
     * Returns the artifact given its id.
     *
     * @param id
     * @return
     *
     */
    protected abstract T getArtifact(String id) throws ClientException;

    /**
     * Returns the type of the current artifact needed for
     * {@link #newObject(String, Object...)}.
     *
     * @return
     *
     */
    protected abstract String getArtifactType();

    /**
     * Checks the precondition to create an artifact (for instance validity,
     * duplicate detection, guards...).
     *
     * @param artifact
     *
     */
    protected abstract void checkPrecondition(T artifact)
            throws ClientException;

    /**
     * Persist an artifact in the underlying persistence system.
     *
     * @param artifact
     * @return
     *
     */
    protected abstract T createArtifact(T artifact) throws ClientException;

    protected void checkCurrentUserCanCreateArtifact(T artifact) {
        NuxeoPrincipal currentUser = (NuxeoPrincipal) getContext().getCoreSession().getPrincipal();
        if (!currentUser.isAdministrator()) {
            if (!currentUser.isMemberOf("powerusers")
                    || !isAPowerUserEditableArtifact(artifact)) {
                throw new WebSecurityException("Cannot create artifact");
            }
        }
    }

    /**
     * Checks that the given artifact
     *
     * @param artifact
     * @param um2
     * @return
     *
     */
    abstract boolean isAPowerUserEditableArtifact(T artifact);

}
