package org.nuxeo.scim.server.mapper;

import java.net.URI;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;

public abstract class AbstractMapper {

    protected UserManager um;

    protected final String baseUrl;

    public AbstractMapper(String baseUrl) {
        um = Framework.getLocalService(UserManager.class);
        this.baseUrl = baseUrl;
    }

    public GroupResource getGroupResourceFromNuxeoGroup(DocumentModel groupModel) throws Exception {

        GroupResource groupResource = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);

        String groupId = (String) groupModel.getProperty(um.getGroupSchemaName(), um.getGroupIdField());

        URI location = new URI(baseUrl + "/" + groupId);
        Meta meta = new Meta(null, null, location, "1");
        groupResource.setMeta(meta);

        String groupLabel = (String) groupModel.getProperty(um.getGroupSchemaName(), um.getGroupLabelField());

        groupResource.setDisplayName(groupLabel);
        groupResource.setId(groupId);
        groupResource.setExternalId(groupId);

        return groupResource;
    }

    public DocumentModel createGroupModelFromGroupResource(GroupResource group) throws NuxeoException {

        if (um.getGroup(group.getId()) == null) {
            DocumentModel newGroup = um.getBareGroupModel();

            String groupId = group.getId();
            if (groupId == null || groupId.isEmpty()) {
                groupId = group.getDisplayName();
            }
            newGroup.setProperty(um.getGroupSchemaName(), um.getGroupIdField(), groupId);
            updateGroupModel(newGroup, group);
            return um.createGroup(newGroup);
        } else {
            return updateGroupModelFromGroupResource(group.getId(), group);
        }
    }

    public DocumentModel updateGroupModelFromGroupResource(String uid, GroupResource group) throws NuxeoException {

        DocumentModel groupModel = um.getGroupModel(uid);
        if (groupModel == null) {
            return null;
        }
        updateGroupModel(groupModel, group);
        um.updateGroup(groupModel);
        return groupModel;
    }

    protected void updateGroupModel(DocumentModel userModel, GroupResource groupResouce) throws NuxeoException {
        String displayName = groupResouce.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            userModel.setProperty(um.getGroupSchemaName(), um.getGroupLabelField(), displayName);
        }
    }

    public abstract UserResource getUserResourceFromNuxeoUser(DocumentModel userModel) throws Exception;

    public abstract DocumentModel createNuxeoUserFromUserResource(UserResource user) throws NuxeoException;

    public abstract DocumentModel updateNuxeoUserFromUserResource(String uid, UserResource user);

}