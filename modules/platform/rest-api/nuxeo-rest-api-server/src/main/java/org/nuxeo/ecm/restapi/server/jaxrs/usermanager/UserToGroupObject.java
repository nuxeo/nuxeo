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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebObject(type = "userToGroup")
public class UserToGroupObject extends DefaultObject {

    private NuxeoGroup group;

    private NuxeoPrincipal principal;

    @Override
    protected void initialize(Object... args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("UserToGroup object takes two parameters");
        }
        principal = (NuxeoPrincipal) args[0];
        group = (NuxeoGroup) args[1];
    }

    @POST
    public Response doAddUserToGroup() {
        UserManager um = Framework.getService(UserManager.class);
        checkPrincipalCanAdministerGroupAndUser(um);
        addUserToGroup(principal, group);
        return Response.status(Status.CREATED).entity(um.getPrincipal(principal.getName())).build();
    }

    private void checkPrincipalCanAdministerGroupAndUser(UserManager um) {
        NuxeoPrincipal currentPrincipal = getContext().getCoreSession().getPrincipal();
        if (!currentPrincipal.isAdministrator()) {
            if (!currentPrincipal.isMemberOf("powerusers") || !UserRootObject.isAPowerUserEditableUser(principal)
                    || !GroupRootObject.isAPowerUserEditableGroup(group)) {
                throw new WebSecurityException("Cannot edit user");
            }
        }

    }

    @DELETE
    public Response doRemoveUserFromGroup() {
        UserManager um = Framework.getService(UserManager.class);
        checkPrincipalCanAdministerGroupAndUser(um);
        removeUserFromGroup(principal, group);
        return Response.ok(principal.getName()).build();
    }

    protected void addUserToGroup(NuxeoPrincipal principal, NuxeoGroup group) {
        UserManager userManager = Framework.getService(UserManager.class);
        if (!BaseSession.isReadOnlyEntry(principal.getModel())) {
            // we can write to the principal
            List<String> groups = principal.getGroups();
            if (groups == null) {
                groups = new ArrayList<>();
            }
            String groupName = group.getName();
            if (!groups.contains(groupName)) {
                groups.add(groupName);
                principal.setGroups(groups);
                userManager.updateUser(principal.getModel());
            }
        } else {
            // principal is read-only, update through the group instead
            List<String> users = group.getMemberUsers();
            if (users == null) {
                users = new ArrayList<>();
            }
            String userName = principal.getName();
            if (!users.contains(userName)) {
                users.add(userName);
                group.setMemberUsers(users);
                userManager.updateGroup(group.getModel());
            }
        }
    }

    protected void removeUserFromGroup(NuxeoPrincipal principal, NuxeoGroup group) {
        UserManager userManager = Framework.getService(UserManager.class);
        if (!BaseSession.isReadOnlyEntry(principal.getModel())) {
            // we can write to the principal
            List<String> groups = principal.getGroups();
            if (groups == null) {
                groups = new ArrayList<>();
            }
            String groupName = group.getName();
            if (groups.contains(groupName)) {
                groups.remove(groupName);
                principal.setGroups(groups);
                userManager.updateUser(principal.getModel());
            }
        } else {
            // principal is read-only, update through the group instead
            List<String> users = group.getMemberUsers();
            if (users == null) {
                users = new ArrayList<>();
            }
            String userName = principal.getName();
            if (users.contains(userName)) {
                users.remove(userName);
                group.setMemberUsers(users);
                userManager.updateGroup(group.getModel());
            }
        }
    }

}
