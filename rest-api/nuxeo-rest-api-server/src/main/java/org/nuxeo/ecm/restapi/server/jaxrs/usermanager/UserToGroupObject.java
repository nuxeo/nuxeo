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

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "userToGroup")
public class UserToGroupObject extends DefaultObject {

    private NuxeoGroup group;

    private NuxeoPrincipal principal;

    @Override
    protected void initialize(Object... args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "UserToGroup object takes two parameters");
        }
        principal = (NuxeoPrincipal) args[0];
        group = (NuxeoGroup) args[1];
    }

    @POST
    public Response doAddUserToGroup() {
        try {
            UserManager um = Framework.getLocalService(UserManager.class);
            checkPrincipalCanAdministerGroupAndUser(um);

            List<String> groups = principal.getGroups();
            groups.add(group.getName());
            principal.setGroups(groups);
            um.updateUser(principal.getModel());
            return Response.status(Status.CREATED).entity(
                    um.getPrincipal(principal.getName())).build();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    private void checkPrincipalCanAdministerGroupAndUser(UserManager um) {
        NuxeoPrincipal currentPrincipal = (NuxeoPrincipal) getContext().getCoreSession().getPrincipal();
        if (!currentPrincipal.isAdministrator()) {
            if (!principal.isMemberOf("powerusers")
                    || !UserRootObject.isAPowerUserEditableUser(principal)
                    || !GroupRootObject.isAPowerUserEditableGroup(group)) {
                throw new WebSecurityException("Cannot edit user");
            }
        }

    }

    @DELETE
    public Response doRemoveUserFromGroup() {
        try {
            UserManager um = Framework.getLocalService(UserManager.class);
            checkPrincipalCanAdministerGroupAndUser(um);
            List<String> groups = principal.getGroups();
            groups.remove(group.getName());
            principal.setGroups(groups);
            um.updateUser(principal.getModel());
            return Response.ok(principal.getName()).build();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }
}
