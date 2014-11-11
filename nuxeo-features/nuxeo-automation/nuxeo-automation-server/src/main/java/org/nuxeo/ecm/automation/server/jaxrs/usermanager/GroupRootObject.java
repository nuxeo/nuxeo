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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "groups")
public class GroupRootObject extends AbstractUMRootObject<NuxeoGroup> {

    @Override
    protected NuxeoGroup getArtifact(String id) throws ClientException {
        return um.getGroup(id);
    }

    @Override
    protected String getArtifactType() {
        return "group";
    }

    @Override
    protected void checkPrecondition(NuxeoGroup group) throws ClientException {
        checkCurrentUserCanCreateArtifact(group);
        checkGroupHasAName(group);
        checkGroupDoesNotAlreadyExists(group, um);
    }

    @Override
    protected NuxeoGroup createArtifact(NuxeoGroup group)
            throws ClientException {
        DocumentModel groupModel = buildModelFromGroup(group, um);

        um.createGroup(groupModel);
        return um.getGroup(group.getName());
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

    @Override
    boolean isAPowerUserEditableArtifact(NuxeoGroup artifact) {
        return isAPowerUserEditableGroup(artifact);

    }

    /**
     * @param group
     * @param um
     * @return
     *
     */
    static boolean isAPowerUserEditableGroup(NuxeoGroup group) {
        UserManager um = Framework.getLocalService(UserManager.class);
        return !um.getAdministratorsGroups().contains(group.getName());

    }

    @Override
    protected List<NuxeoGroup> searchArtifact(String query)
            throws ClientException {
        List<DocumentModel> searchUsers = um.searchGroups(query);
        List<NuxeoGroup> groups = new ArrayList<>(searchUsers.size());
        for (DocumentModel userDoc : searchUsers) {
            NuxeoGroup group = um.getGroup(userDoc.getProperty(
                    um.getGroupIdField()).getValue(String.class));
            groups.add(group);
        }
        return groups;

    }

}
