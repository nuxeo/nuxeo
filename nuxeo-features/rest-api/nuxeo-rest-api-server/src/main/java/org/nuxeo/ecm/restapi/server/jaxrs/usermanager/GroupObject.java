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

import java.io.Serializable;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "group")
public class GroupObject extends AbstractUMObject<NuxeoGroup> {

    @Path("user/{username}")
    public Object doGetUserToGroup(@PathParam("username")
    String username) {
        try {
            UserManager um = Framework.getLocalService(UserManager.class);
            NuxeoPrincipal principal = um.getPrincipal(username);
            if (principal == null) {
                throw new WebResourceNotFoundException("User not found");
            }
            return newObject("userToGroup", principal, currentArtifact);

        } catch (ClientException e) {
            throw WebException.wrap(e);
        }

    }

    @Override
    protected NuxeoGroup updateArtifact(NuxeoGroup updateGroup)
            throws ClientException {
        DocumentModel groupModel = um.getGroupModel(currentArtifact.getName());
        groupModel.setPropertyValue(um.getGroupLabelField(),
                updateGroup.getLabel());
        groupModel.setPropertyValue(um.getGroupMembersField(),
                (Serializable) updateGroup.getMemberUsers());
        groupModel.setPropertyValue(um.getGroupSubGroupsField(),
                (Serializable) updateGroup.getMemberGroups());

        um.updateGroup(groupModel);
        return um.getGroup(currentArtifact.getName());
    }

    @Override
    protected void deleteArtifact() throws ClientException {
        um.deleteGroup(um.getGroupModel(currentArtifact.getName()));
    }

    @Override
    protected boolean isAPowerUserEditableArtifact() {
        return GroupRootObject.isAPowerUserEditableGroup(currentArtifact);
    }
}
