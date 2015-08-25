package org.nuxeo.scim.server.jaxrs.usermanager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.SCIMConstants;

public class UserMapper {

    protected UserManager um;

    protected final String baseUrl;

    public UserMapper(String baseUrl) {
        um = Framework.getLocalService(UserManager.class);
        this.baseUrl = baseUrl;
    }

    public GroupResource getGroupResourceFromGroupModel(DocumentModel groupModel)
            throws Exception {

        GroupResource groupResource = new GroupResource(
                CoreSchema.GROUP_DESCRIPTOR);

        String groupId = (String) groupModel.getProperty(
                um.getGroupSchemaName(), um.getGroupIdField());

        String groupLabel = (String) groupModel.getProperty(
                um.getGroupSchemaName(), um.getGroupLabelField());

        groupResource.setDisplayName(groupLabel);
        groupResource.setId(groupId);
        groupResource.setExternalId(groupId);

        URI location = new URI(baseUrl + "/" + groupId);
        Meta meta = new Meta(null, null, location, "1");
        groupResource.setMeta(meta);
        // XXX

        return groupResource;
    }


    public UserResource getUserResourcefromUserModel(DocumentModel userModel)
            throws Exception {

        UserResource userResource = new UserResource(CoreSchema.USER_DESCRIPTOR);

        String userId = (String) userModel.getProperty(um.getUserSchemaName(),
                um.getUserIdField());
        userResource.setUserName(userId);
        userResource.setId(userId);
        userResource.setExternalId(userId);

        String fname = (String) userModel.getProperty(um.getUserSchemaName(),
                "firstName");
        String lname = (String) userModel.getProperty(um.getUserSchemaName(),
                "lastName");
        String email = (String) userModel.getProperty(um.getUserSchemaName(),
                "email");
        String company = (String) userModel.getProperty(um.getUserSchemaName(),
                "company");

        String displayName = fname + " " + lname;
        displayName = displayName.trim();
        userResource.setDisplayName(displayName);
        Collection<Entry<String>> emails = new ArrayList<>();
        if (email!=null) {
            emails.add(new Entry<String>(email, "string"));
            userResource.setEmails(emails);
        }

        Name fullName = new Name(displayName, lname, "", fname, "", "");
        userResource.setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
                "name", Name.NAME_RESOLVER, fullName);
        URI location = new URI(baseUrl + "/" + userId);
        Meta meta = new Meta(null, null, location, "1");
        userResource.setMeta(meta);

        // manage groups
        List<String> groupIds = um.getPrincipal(userId).getAllGroups();
        Collection<Entry<String>> groups = new ArrayList<>();
        for (String groupId : groupIds) {
            groups.add(new Entry<String>(groupId, "string"));
        }
        userResource.setGroups(groups);

        userResource.setActive(true);

        return userResource;
    }

    public DocumentModel createUserModelFromUserResource(UserResource user)
            throws NuxeoException {

        DocumentModel newUser = um.getBareUserModel();

        String userId = user.getId();
        if (userId == null || userId.isEmpty()) {
            userId = user.getUserName();
        }
        newUser.setProperty(um.getUserSchemaName(), um.getUserIdField(), userId);

        updateUserModel(newUser, user);
        return um.createUser(newUser);
    }

    public DocumentModel updateUserModelFromUserResource(String uid, UserResource user)
            throws NuxeoException {

        DocumentModel userModel = um.getUserModel(uid);
        if (userModel == null) {
            return null;
        }
        updateUserModel(userModel, user);
        um.updateUser(userModel);
        return userModel;
    }

    protected void updateUserModel(DocumentModel userModel,
            UserResource userResouce) throws NuxeoException {
        if (userResouce.getEmails() != null
                && userResouce.getEmails().size() > 0) {
            userModel.setProperty(um.getUserSchemaName(), "email",
                    userResouce.getEmails().iterator().next().getValue());
        }
        String displayName = userResouce.getDisplayName();
        if (displayName!=null && !displayName.isEmpty()) {
            int idx = displayName.indexOf(" ");
            if (idx>0) {
                userModel.setProperty(um.getUserSchemaName(),
                        "firstName", displayName.substring(0, idx).trim());
                userModel.setProperty(um.getUserSchemaName(),
                        "lastName", displayName.substring(idx+1).trim());
            } else {
                userModel.setProperty(um.getUserSchemaName(),
                        "firstName", displayName);
                userModel.setProperty(um.getUserSchemaName(),
                        "lastName", "");
            }
        }

        // XXX
    }

    public DocumentModel createGroupModelFromGroupResource(GroupResource group)
            throws NuxeoException {

        if (um.getGroup(group.getId())==null) {
            DocumentModel newGroup = um.getBareGroupModel();

            String groupId = group.getId();
            if (groupId == null || groupId.isEmpty()) {
                groupId = group.getDisplayName();
            }
            newGroup.setProperty(um.getGroupSchemaName(), um.getGroupIdField(),
                    groupId);
            updateGroupModel(newGroup, group);
            return um.createGroup(newGroup);
        } else {
            return updateGroupModelFromGroupResource(group.getId(), group);
        }
    }

    public DocumentModel updateGroupModelFromGroupResource(String uid, GroupResource group)
            throws NuxeoException {

        DocumentModel groupModel = um.getGroupModel(uid);
        if (groupModel == null) {
            return null;
        }
        updateGroupModel(groupModel, group);
        um.updateGroup(groupModel);
        return groupModel;
    }

    protected void updateGroupModel(DocumentModel userModel,
            GroupResource groupResouce) throws NuxeoException {
        String displayName = groupResouce.getDisplayName();
        if (displayName!=null && !displayName.isEmpty()) {
            userModel.setProperty(um.getGroupSchemaName(), um.getGroupLabelField(), displayName);
        }
    }


}
