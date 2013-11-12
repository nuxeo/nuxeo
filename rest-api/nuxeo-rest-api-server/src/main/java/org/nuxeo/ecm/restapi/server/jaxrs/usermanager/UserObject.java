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

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "user")
@Produces({ MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_JSON + "+nxentity" })
public class UserObject extends AbstractUMObject<NuxeoPrincipal> {

    @Path("group/{groupName}")
    public Object doGetUserToGroup(@PathParam("groupName")
    String groupName) {
        try {
            NuxeoGroup group = um.getGroup(groupName);
            if (group == null) {
                throw new WebResourceNotFoundException("Group not found");
            }

            return newObject("userToGroup", currentArtifact, group);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @Override
    protected NuxeoPrincipal updateArtifact(NuxeoPrincipal principal)
            throws ClientException {
        um.updateUser(principal.getModel());
        return um.getPrincipal(principal.getName());
    }

    @Override
    protected void deleteArtifact() throws ClientException {
        um.deleteUser(currentArtifact.getModel());
    }

    @Override
    protected boolean isAPowerUserEditableArtifact() {
        return UserRootObject.isAPowerUserEditableUser(currentArtifact);

    }

}
