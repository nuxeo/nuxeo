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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserAdapter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter;

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

    /*
     * @since 11.4
     */
    @Param(name = "allowSubGroupsRestriction", required = false, description = "Whether to take into account subgroups when evaluating groupRestriction.")
    protected boolean allowSubGroupsRestriction;

    @OperationMethod
    public Blob run() throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
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
        int limit = userSuggestionMaxSearchResults == null ? 0 : userSuggestionMaxSearchResults.intValue();
        try {
            int userSize = 0;
            int groupSize = 0;
            if (!groupOnly) {
                if (limit > 0 && isGroupRestriction) {
                    // we may have to iterate several times, while increasing the limit,
                    // because the group restrictions may truncate our results
                    long currentLimit = limit;
                    int prevUserListSize = -1;
                    for (;;) {
                        DocumentModelList userList = searchUsers(currentLimit);
                        result = usersToMapWithGroupRestrictions(userList);
                        int userListSize = userList.size();
                        if (userListSize == prevUserListSize || result.size() > limit) {
                            // stop if the search didn't return more results
                            // or if we are beyond the limit anyway
                            break;
                        }
                        prevUserListSize = userListSize;
                        currentLimit *= 2;
                        if (currentLimit > Integer.MAX_VALUE) {
                            break;
                        }
                    }
                } else {
                    DocumentModelList userList = searchUsers(limit);
                    result = usersToMapWithGroupRestrictions(userList);
                }
                userSize = result.size();
            }
            if (!userOnly) {
                Schema schema = schemaManager.getSchema(userManager.getGroupSchemaName());
                DocumentModelList groupList = userManager.searchGroups(prefix);
                List<String> admins = new ArrayList<>();
                if (hideAdminGroups) {
                    admins = userManager.getAdministratorsGroups();
                }
                groupLoop: for (DocumentModel group : groupList) {
                    if (hideAdminGroups) {
                        for (String adminGroupName : admins) {
                            if (adminGroupName.equals(group.getId())) {
                                break groupLoop;
                            }
                        }
                    }
                    if (hidePowerUsersGroup && POWERUSERS.equals(group.getId())) {
                        break groupLoop;
                    }
                    Map<String, Object> obj = new LinkedHashMap<>();
                    for (Field field : schema.getFields()) {
                        QName fieldName = field.getName();
                        String key = fieldName.getLocalName();
                        Serializable value = group.getPropertyValue(fieldName.getPrefixedName());
                        obj.put(key, value);
                    }
                    String groupId = group.getId();
                    obj.put(SuggestConstants.ID, groupId);
                    obj.put(MarshallingConstants.ENTITY_FIELD_NAME, NuxeoGroupJsonWriter.ENTITY_TYPE);
                    // If the group hasn't an label, let's put the groupid
                    SuggestConstants.computeGroupLabel(obj, groupId, userManager.getGroupLabelField(), hideFirstLabel);
                    obj.put(SuggestConstants.TYPE_KEY_NAME, SuggestConstants.GROUP_TYPE);
                    obj.put(SuggestConstants.PREFIXED_ID_KEY_NAME, NuxeoGroup.PREFIX + groupId);
                    SuggestConstants.computeUserGroupIcon(obj, hideIcon);
                    result.add(obj);
                }
                groupSize = result.size() - userSize;
            }

            // Limit size results.
            if (limit > 0 && (userSize > limit || groupSize > limit || userSize + groupSize > limit)) {
                throw new SizeLimitExceededException();
            }

        } catch (SizeLimitExceededException e) {
            return searchOverflowMessage();
        }

        return Blobs.createJSONBlobFromValue(result);
    }

    /**
     * Applies group restrictions, and returns Map objects.
     */
    protected List<Map<String, Object>> usersToMapWithGroupRestrictions(DocumentModelList userList) {
        List<Map<String, Object>> result = new ArrayList<>();
        Schema schema = schemaManager.getSchema(userManager.getUserSchemaName());
        Directory userDir = directoryService.getDirectory(userManager.getUserDirectoryName());
        for (DocumentModel user : userList) {
            Map<String, Object> obj = new LinkedHashMap<>();
            for (Field field : schema.getFields()) {
                QName fieldName = field.getName();
                String key = fieldName.getLocalName();
                Serializable value = user.getPropertyValue(fieldName.getPrefixedName());
                if (key.equals(userDir.getPasswordField())) {
                    continue;
                }
                obj.put(key, value);
            }
            String userId = user.getId();
            obj.put(SuggestConstants.ID, userId);
            obj.put(MarshallingConstants.ENTITY_FIELD_NAME, NuxeoPrincipalJsonWriter.ENTITY_TYPE);
            obj.put(SuggestConstants.TYPE_KEY_NAME, SuggestConstants.USER_TYPE);
            obj.put(SuggestConstants.PREFIXED_ID_KEY_NAME, NuxeoPrincipal.PREFIX + userId);
            SuggestConstants.computeUserLabel(obj, firstLabelField, secondLabelField, thirdLabelField,
                    hideFirstLabel, hideSecondLabel, hideThirdLabel, displayEmailInSuggestion, userId);
            SuggestConstants.computeUserGroupIcon(obj, hideIcon);
            if (!StringUtils.isBlank(groupRestriction)) {
                // We need to load all data about the user particularly its groups.
                user = userManager.getUserModel(userId);
                UserAdapter userAdapter = user.getAdapter(UserAdapter.class);
                List<String> groups = userAdapter.getGroups();
                if (CollectionUtils.isNotEmpty(groups)) {
                    List<String> restrictedGroups = new ArrayList<>();
                    restrictedGroups.add(groupRestriction);
                    if (allowSubGroupsRestriction) {
                        restrictedGroups.addAll(userManager.getDescendantGroups(groupRestriction));
                    }
                    restrictedGroups.retainAll(groups);
                    if (!restrictedGroups.isEmpty()) {
                        result.add(obj);
                    }
                }
            } else {
                result.add(obj);
            }
        }
        return result;
    }

    /**
     * Performs a full name user search, e.g. typing "John Do" returns the user with first name "John" and last name
     * "Doe". Typing "John" returns the "John Doe" user and possibly other users such as "John Foo". Respectively,
     * typing "Do" returns the "John Doe" user and possibly other users such as "Jack Donald".
     */
    protected DocumentModelList searchUsers(long limit) {
        if (StringUtils.isBlank(prefix)) {
            // empty search term
            return userManager.searchUsers(prefix);
        }
        // split search term around whitespace, e.g. "John Do" -> ["John", "Do"]
        String[] searchTerms = prefix.trim().split("\\s", 2);
        List<Predicate> predicates = Arrays.stream(searchTerms)
                                           .map(this::getUserSearchPredicate)
                                           .collect(Collectors.toList());
        // intersection between all search results to handle full name
        DocumentModelList users = searchUsers(new MultiExpression(Operator.AND, predicates), limit);
        if (users.isEmpty()) {
            // search on whole term to handle a whitespace within the first or last name
            users = searchUsers(getUserSearchPredicate(prefix), limit);
        }
        return users;
    }

    protected DocumentModelList searchUsers(MultiExpression multiExpression, long limit) {
        QueryBuilder queryBuilder = new QueryBuilder();
        queryBuilder.filter(multiExpression);
        if (limit > 0) {
            // no need to search more than that because we throw SizeLimitExceededException is we reach it
            queryBuilder.limit(limit + 1);
        }
        String sortField = StringUtils.defaultString(userManager.getUserSortField(), userManager.getUserIdField());
        queryBuilder.order(OrderByExprs.asc(sortField));
        return userManager.searchUsers(queryBuilder);
    }

    protected MultiExpression getUserSearchPredicate(String prefix) {
        String pattern = prefix.trim() + '%';
        List<Predicate> predicates = userManager.getUserSearchFields()
                          .stream()
                          .map(key -> Predicates.ilike(key, pattern))
                          .collect(Collectors.toList());
        return new MultiExpression(Operator.OR, predicates);
    }

    /**
     * @return searchOverflowMessage
     * @since 5.7.3
     */
    private Blob searchOverflowMessage() throws IOException {
        String label = I18NUtils.getMessageString("messages", "label.security.searchOverFlow", new Object[0],
                getLocale());
        Map<String, Object> obj = Collections.singletonMap(SuggestConstants.LABEL, label);
        return Blobs.createJSONBlobFromValue(Collections.singletonList(obj));
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
