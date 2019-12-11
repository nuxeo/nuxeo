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

import java.io.Serializable;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebObject(type = "group")
public class GroupObject extends AbstractUMObject<NuxeoGroup> {

    @Path("user/{username}")
    public Object doGetUserToGroup(@PathParam("username") String username) {
        try {
            UserManager um = Framework.getLocalService(UserManager.class);
            NuxeoPrincipal principal = um.getPrincipal(username);
            if (principal == null) {
                throw new WebResourceNotFoundException("User not found");
            }
            return newObject("userToGroup", principal, currentArtifact);

        } catch (NuxeoException e) {
            throw WebException.wrap(e);
        }

    }

    @Override
    protected NuxeoGroup updateArtifact(NuxeoGroup updateGroup) {
        DocumentModel groupModel = um.getGroupModel(currentArtifact.getName());

        DocumentModel model = updateGroup.getModel();
        if (model != null) {
            String groupSchemaName = um.getGroupSchemaName();
            model.getPropertyObjects(groupSchemaName).stream().filter(Property::isDirty).forEach(
                    (property) -> groupModel.setProperty(groupSchemaName, property.getName(), property.getValue()));
        }

        // make sure to override group properties for compat
        groupModel.setPropertyValue(um.getGroupLabelField(), updateGroup.getLabel());
        groupModel.setPropertyValue(um.getGroupMembersField(), (Serializable) updateGroup.getMemberUsers());
        groupModel.setPropertyValue(um.getGroupSubGroupsField(), (Serializable) updateGroup.getMemberGroups());
        groupModel.setPropertyValue(um.getGroupParentGroupsField(), (Serializable) updateGroup.getParentGroups());

        um.updateGroup(groupModel);
        return um.getGroup(currentArtifact.getName());
    }

    @Override
    protected void deleteArtifact() {
        um.deleteGroup(um.getGroupModel(currentArtifact.getName()));
    }

    @Override
    protected boolean isAPowerUserEditableArtifact(NuxeoGroup artifact) {
        return GroupRootObject.isAPowerUserEditableGroup(artifact);
    }
}
