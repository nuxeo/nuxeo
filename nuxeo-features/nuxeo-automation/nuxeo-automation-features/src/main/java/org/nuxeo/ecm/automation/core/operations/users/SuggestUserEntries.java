/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.automation.core.operations.users;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.features.SuggestConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserAdapter;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * SuggestUser Operation.
 *
 * @since 5.7.3
 */
@Operation(id = SuggestUserEntries.ID, category = Constants.CAT_SERVICES, label = "Get user/group suggestion", description = "Get the user/group list of the running instance. This is returning a blob containing a serialized JSON array..", addToStudio = false)
public class SuggestUserEntries {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(SuggestUserEntries.class);

    public static final String ID = "UserGroup.Suggestion";

    public static final String POWERUSERS = "powerusers";

    @Context
    protected OperationContext ctx;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "searchTerm", alias = "prefix", required = false)
    protected String prefix;

    @Param(name = "searchType", required = false)
    protected String searchType;

    @Param(name = "groupRestriction", required = false, description = "Enter the id of a group to suggest only user from this group.")
    protected String groupRestriction;

    /**
     * @since 7.10
     */
    @Param(name = "hideAdminGroups", required = false, description = "If set, remove all administrator groups from the suggestions")
    protected boolean hideAdminGroups;

    /**
     * @since 8.3
     */
    @Param(name = "hidePowerUsersGroup", required = false, description = "If set, remove power users group from the suggestions")
    protected boolean hidePowerUsersGroup;

    @Param(name = "userSuggestionMaxSearchResults", required = false)
    protected Integer userSuggestionMaxSearchResults;

    @Param(name = "firstLabelField", required = false)
    protected String firstLabelField;

    @Param(name = "secondLabelField", required = false)
    protected String secondLabelField;

    @Param(name = "thirdLabelField", required = false)
    protected String thirdLabelField;

    @Param(name = "hideFirstLabel", required = false)
    protected boolean hideFirstLabel = false;

    @Param(name = "hideSecondLabel", required = false)
    protected boolean hideSecondLabel = false;

    @Param(name = "hideThirdLabel", required = false)
    protected boolean hideThirdLabel;

    @Param(name = "displayEmailInSuggestion", required = false)
    protected boolean displayEmailInSuggestion;

    @Param(name = "hideIcon", required = false)
    protected boolean hideIcon;

    @Context
    protected UserManager userManager;

    @Context
    protected DirectoryService directoryService;

    @Param(name = "lang", required = false)
    protected String lang;

    @OperationMethod
    public Blob run() {
        JSONArray result = new JSONArray();
        boolean isGroupRestriction = !StringUtils.isBlank(groupRestriction);
        boolean groupOnly = false;
        boolean userOnly = isGroupRestriction;

        if (!isGroupRestriction && searchType != null && !searchType.isEmpty()) {
            if (searchType.equals(SuggestConstants.USER_TYPE)) {
                userOnly = true;
            } else if (searchType.equals(SuggestConstants.GROUP_TYPE)) {
                groupOnly = true;
            }
        }
        try {
            DocumentModelList userList = null;
            DocumentModelList groupList = null;
            if (!groupOnly) {
                Schema schema = schemaManager.getSchema(userManager.getUserSchemaName());
                userList = userManager.searchUsers(prefix);
                Directory userDir = directoryService.getDirectory(userManager.getUserDirectoryName());
                for (DocumentModel user : userList) {
                    JSONObject obj = new JSONObject();
                    for (Field field : schema.getFields()) {
                        QName fieldName = field.getName();
                        String key = fieldName.getLocalName();
                        Serializable value = user.getPropertyValue(fieldName.getPrefixedName());
                        if (key.equals(userDir.getPasswordField())) {
                            continue;
                        }
                        obj.element(key, value);
                    }
                    String userId = user.getId();
                    obj.put(SuggestConstants.ID, userId);
                    obj.put(SuggestConstants.TYPE_KEY_NAME, SuggestConstants.USER_TYPE);
                    obj.put(SuggestConstants.PREFIXED_ID_KEY_NAME, NuxeoPrincipal.PREFIX + userId);
                    SuggestConstants.computeUserLabel(obj, firstLabelField, secondLabelField, thirdLabelField,
                            hideFirstLabel, hideSecondLabel, hideThirdLabel, displayEmailInSuggestion, userId);
                    SuggestConstants.computeUserGroupIcon(obj, hideIcon);
                    if (isGroupRestriction) {
                        // We need to load all data about the user particularly
                        // its
                        // groups.
                        user = userManager.getUserModel(userId);
                        UserAdapter userAdapter = user.getAdapter(UserAdapter.class);
                        List<String> groups = userAdapter.getGroups();
                        if (groups != null && groups.contains(groupRestriction)) {
                            result.add(obj);
                        }
                    } else {
                        result.add(obj);
                    }
                }
            }
            if (!userOnly) {
                Schema schema = schemaManager.getSchema(userManager.getGroupSchemaName());
                groupList = userManager.searchGroups(prefix);
                List<String> admins = new ArrayList<>();
                if (hideAdminGroups) {
                    admins = userManager.getAdministratorsGroups();
                }
                groupLoop:
                for (DocumentModel group : groupList) {
                    if (hideAdminGroups) {
                        for (String adminGroupName : admins) {
                            if (adminGroupName.equals(group.getId())) {
                                break groupLoop;
                            }
                        }
                    }
                    if (hidePowerUsersGroup) {
                        if (POWERUSERS.equals(group.getId())) {
                            break groupLoop;
                        }
                    }
                    JSONObject obj = new JSONObject();
                    for (Field field : schema.getFields()) {
                        QName fieldName = field.getName();
                        String key = fieldName.getLocalName();
                        Serializable value = group.getPropertyValue(fieldName.getPrefixedName());
                        obj.element(key, value);
                    }
                    String groupId = group.getId();
                    obj.put(SuggestConstants.ID, groupId);
                    // If the group hasn't an label, let's put the groupid
                    SuggestConstants.computeGroupLabel(obj, groupId, userManager.getGroupLabelField(), hideFirstLabel);
                    obj.put(SuggestConstants.TYPE_KEY_NAME, SuggestConstants.GROUP_TYPE);
                    obj.put(SuggestConstants.PREFIXED_ID_KEY_NAME, NuxeoGroup.PREFIX + groupId);
                    SuggestConstants.computeUserGroupIcon(obj, hideIcon);
                    result.add(obj);
                }
            }

            // Limit size results.
            int userSize = userList != null ? userList.size() : 0;
            int groupSize = groupList != null ? groupList.size() : 0;
            int totalSize = userSize + groupSize;
            if (userSuggestionMaxSearchResults != null && userSuggestionMaxSearchResults > 0) {
                if (userSize > userSuggestionMaxSearchResults || groupSize > userSuggestionMaxSearchResults
                        || totalSize > userSuggestionMaxSearchResults) {
                    throw new SizeLimitExceededException();
                }
            }

        } catch (SizeLimitExceededException e) {
            return searchOverflowMessage();
        }

        return Blobs.createJSONBlob(result.toString());
    }

    /**
     * @return searchOverflowMessage
     * @since 5.7.3
     */
    private Blob searchOverflowMessage() {
        JSONArray result = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put(SuggestConstants.LABEL,
                I18NUtils.getMessageString("messages", "label.security.searchOverFlow", new Object[0], getLocale()));
        result.add(obj);
        return Blobs.createJSONBlob(result.toString());
    }

    protected String getLang() {
        if (lang == null) {
            lang = (String) ctx.get("lang");
            if (lang == null) {
                lang = SuggestConstants.DEFAULT_LANG;
            }
        }
        return lang;
    }

    protected Locale getLocale() {
        return new Locale(getLang());
    }

}
