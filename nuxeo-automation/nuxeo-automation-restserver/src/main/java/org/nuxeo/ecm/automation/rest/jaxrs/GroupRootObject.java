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
package org.nuxeo.ecm.automation.rest.jaxrs;

import java.io.Serializable;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "groups")
public class GroupRootObject extends DefaultObject {

    @Path("{groupName}")
    public Object doGetGroup(@PathParam("groupName")
    String groupName) {
        try {
            UserManager um = Framework.getLocalService(UserManager.class);
            NuxeoGroup group = um.getGroup(groupName);
            if (group == null) {
                throw new WebResourceNotFoundException("Group does not exist");
            }
            return newObject("group", group);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    public Response doCreateGroup(NuxeoGroup group) {
        try {
            UserManager um = Framework.getLocalService(UserManager.class);

            checkGroupHasAName(group);
            checkGroupDoesNotAlreadyExists(group, um);

            DocumentModel groupModel = buildModelFromGroup(group, um);

            um.createGroup(groupModel);
            return Response.status(Status.CREATED).entity(
                    um.getGroup(group.getName())).build();

        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * Builds a DocumentModel from a group suitable to call UserManager methods.
     *
     * @param group
     * @param um
     * @return
     * @throws ClientException
     *
     */
    private DocumentModel buildModelFromGroup(NuxeoGroup group, UserManager um)
            throws ClientException {
        DocumentModel groupModel = um.getBareGroupModel();
        String schemaName = um.getGroupSchemaName();
        groupModel.setProperty(schemaName, um.getGroupIdField(),
                group.getName());
        groupModel.setProperty(schemaName, um.getGroupLabelField(),
                group.getLabel());

        groupModel.setPropertyValue(um.getGroupMembersField(),
                (Serializable) group.getMemberUsers());
        groupModel.setPropertyValue(um.getGroupSubGroupsField(),
                (Serializable) group.getMemberGroups());
        return groupModel;
    }

    /**
     * @param group
     * @param um
     * @throws ClientException
     *
     */
    private void checkGroupDoesNotAlreadyExists(NuxeoGroup group, UserManager um)
            throws ClientException {
        if (um.getGroup(group.getName()) != null) {
            throw new WebException("Group already exists",
                    Response.Status.PRECONDITION_FAILED.getStatusCode());
        }
    }

    /**
     * @param group
     *
     */
    private void checkGroupHasAName(NuxeoGroup group) {
        if (group.getName() == null) {
            throw new WebException("Group MUST have a name",
                    Response.Status.PRECONDITION_FAILED.getStatusCode());
        }
    }

}
