/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.mapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.usermapper.service.UserMapperService;

import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.SCIMConstants;

/**
 * Static / Hardcoded Mapper implementation (in case {@link UserMapperService} is not available)
 *
 * @author tiry
 * @since 7.4
 */
public class StaticUserMapper extends AbstractMapper {

    public StaticUserMapper(String baseUrl) {
        super(baseUrl);
    }

    @Override
    public UserResource getUserResourceFromNuxeoUser(DocumentModel userModel) throws Exception {

        UserResource userResource = new UserResource(CoreSchema.USER_DESCRIPTOR);

        String userId = (String) userModel.getProperty(um.getUserSchemaName(), um.getUserIdField());
        userResource.setUserName(userId);
        userResource.setId(userId);
        userResource.setExternalId(userId);

        String fname = (String) userModel.getProperty(um.getUserSchemaName(), "firstName");
        String lname = (String) userModel.getProperty(um.getUserSchemaName(), "lastName");
        String email = (String) userModel.getProperty(um.getUserSchemaName(), "email");
        userModel.getProperty(um.getUserSchemaName(), "company");

        String displayName = fname + " " + lname;
        displayName = displayName.trim();
        userResource.setDisplayName(displayName);
        Collection<Entry<String>> emails = new ArrayList<>();
        if (email != null) {
            emails.add(new Entry<>(email, "string"));
            userResource.setEmails(emails);
        }

        Name fullName = new Name(displayName, lname, "", fname, "", "");
        userResource.setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "name", Name.NAME_RESOLVER, fullName);
        URI location = new URI(baseUrl + "/" + userId);
        Meta meta = new Meta(null, null, location, "1");
        userResource.setMeta(meta);

        // manage groups
        List<String> groupIds = um.getPrincipal(userId).getAllGroups();
        Collection<Entry<String>> groups = new ArrayList<>();
        for (String groupId : groupIds) {
            groups.add(new Entry<>(groupId, "string"));
        }
        userResource.setGroups(groups);

        userResource.setActive(true);

        return userResource;
    }

    @Override
    public DocumentModel createNuxeoUserFromUserResource(UserResource user) throws NuxeoException {

        DocumentModel newUser = um.getBareUserModel();

        String userId = user.getId();
        if (userId == null || userId.isEmpty()) {
            userId = user.getUserName();
        }
        newUser.setProperty(um.getUserSchemaName(), um.getUserIdField(), userId);

        updateUserModel(newUser, user);
        return um.createUser(newUser);
    }

    @Override
    public DocumentModel updateNuxeoUserFromUserResource(String uid, UserResource user) throws NuxeoException {

        DocumentModel userModel = um.getUserModel(uid);
        if (userModel == null) {
            return null;
        }
        updateUserModel(userModel, user);
        um.updateUser(userModel);
        return userModel;
    }

    protected void updateUserModel(DocumentModel userModel, UserResource userResouce) throws NuxeoException {
        if (userResouce.getEmails() != null && userResouce.getEmails().size() > 0) {
            userModel.setProperty(um.getUserSchemaName(), "email", userResouce.getEmails().iterator().next().getValue());
        }
        String displayName = userResouce.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            int idx = displayName.indexOf(" ");
            if (idx > 0) {
                userModel.setProperty(um.getUserSchemaName(), "firstName", displayName.substring(0, idx).trim());
                userModel.setProperty(um.getUserSchemaName(), "lastName", displayName.substring(idx + 1).trim());
            } else {
                userModel.setProperty(um.getUserSchemaName(), "firstName", displayName);
                userModel.setProperty(um.getUserSchemaName(), "lastName", "");
            }
        }

        // XXX
    }

}
