/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.PAGE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.convert.Converter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Methods to get user/groups suggestions from searches.
 *
 * @author Anahide Tchertchian
 */
@Name("userSuggestionActions")
@Scope(PAGE)
public class UserSuggestionActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserSuggestionActionsBean.class);

    public static final String USER_TYPE = "USER_TYPE";

    public static final String GROUP_TYPE = "GROUP_TYPE";

    public static final String TYPE_KEY_NAME = "type";

    public static final String PREFIXED_ID_KEY_NAME = "prefixed_id";

    public static final String ID_KEY_NAME = "id";

    public static final String ENTRY_KEY_NAME = "entry";

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @RequestParameter
    protected String userSuggestionSearchType;

    protected String cachedUserSuggestionSearchType;

    @RequestParameter
    protected Integer userSuggestionMaxSearchResults;

    @RequestParameter
    protected Boolean hideVirtualGroups;

    protected Integer cachedUserSuggestionMaxSearchResults;

    protected Object cachedInput;

    protected Object cachedSuggestions;

    @RequestParameter
    protected String userSuggestionMessageId;

    /**
     * Id of the editable list component where selection ids are put.
     * <p>
     * Component must be an instance of {@link UIEditableList}
     */
    @RequestParameter
    protected String suggestionSelectionListId;

    protected void addSearchOverflowMessage() {
        if (userSuggestionMessageId != null) {
            facesMessages.addToControl(userSuggestionMessageId, StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("label.security.searchOverFlow"));
        } else {
            log.error("Search overflow");
        }
    }

    public List<DocumentModel> getGroupsSuggestions(Object input) {
        try {
            Map<String, DocumentModel> uniqueGroups = new HashMap<String, DocumentModel>();

            String pattern = (String) input;
            for (String field : userManager.getGroupSearchFields()) {
                // XXX: this doesn't fetch group members (references)
                Map<String, Serializable> filter = new HashMap<String, Serializable>();

                if (pattern != null && pattern != "") {
                    filter.put(field, pattern);
                }
                if (Boolean.TRUE.equals(hideVirtualGroups)) {
                    filter.put("__virtualGroup", false);
                }

                for (DocumentModel group : userManager.searchGroups(filter, filter.keySet())) {
                    uniqueGroups.put(group.getId(), group);
                }
            }

            DocumentModelList groups = new DocumentModelListImpl();
            groups.addAll(uniqueGroups.values());
            Collections.sort(groups, new DocumentModelComparator(userManager.getGroupSchemaName(), getGroupsOrderBy()));
            return groups;
        } catch (SizeLimitExceededException e) {
            addSearchOverflowMessage();
            return Collections.emptyList();
        }
    }

    protected Map<String, String> getGroupsOrderBy() {
        Map<String, String> order = new HashMap<String, String>();
        order.put(userManager.getGroupLabelField(), DocumentModelComparator.ORDER_ASC);
        return order;
    }

    public List<DocumentModel> getUserSuggestions(Object input) {
        try {
            String searchPattern = (String) input;
            return userManager.searchUsers(searchPattern);
        } catch (SizeLimitExceededException e) {
            addSearchOverflowMessage();
            return Collections.emptyList();
        }
    }

    protected boolean equals(Object item1, Object item2) {
        if (item1 == null && item2 == null) {
            return true;
        } else if (item1 == null) {
            return false;
        } else {
            return item1.equals(item2);
        }
    }

    public Object getSuggestions(Object input) {
        if (equals(cachedUserSuggestionSearchType, userSuggestionSearchType)
                && equals(cachedUserSuggestionMaxSearchResults, userSuggestionMaxSearchResults)
                && equals(cachedInput, input)) {
            return cachedSuggestions;
        }

        List<DocumentModel> users = Collections.emptyList();
        if (USER_TYPE.equals(userSuggestionSearchType) || StringUtils.isEmpty(userSuggestionSearchType)) {
            users = getUserSuggestions(input);
        }

        List<DocumentModel> groups = Collections.emptyList();
        if (GROUP_TYPE.equals(userSuggestionSearchType) || StringUtils.isEmpty(userSuggestionSearchType)) {
            groups = getGroupsSuggestions(input);
        }

        int userSize = users.size();
        int groupSize = groups.size();
        int totalSize = userSize + groupSize;

        if (userSuggestionMaxSearchResults != null && userSuggestionMaxSearchResults > 0) {
            if (userSize > userSuggestionMaxSearchResults || groupSize > userSuggestionMaxSearchResults
                    || totalSize > userSuggestionMaxSearchResults) {
                addSearchOverflowMessage();
                return null;
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(totalSize);

        for (DocumentModel user : users) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put(TYPE_KEY_NAME, USER_TYPE);
            entry.put(ENTRY_KEY_NAME, user);
            String userId = user.getId();
            entry.put(ID_KEY_NAME, userId);
            entry.put(PREFIXED_ID_KEY_NAME, NuxeoPrincipal.PREFIX + userId);
            result.add(entry);
        }

        for (DocumentModel group : groups) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put(TYPE_KEY_NAME, GROUP_TYPE);
            entry.put(ENTRY_KEY_NAME, group);
            String groupId = group.getId();
            entry.put(ID_KEY_NAME, groupId);
            entry.put(PREFIXED_ID_KEY_NAME, NuxeoGroup.PREFIX + groupId);
            result.add(entry);
        }

        cachedInput = input;
        cachedUserSuggestionSearchType = userSuggestionSearchType;
        cachedUserSuggestionMaxSearchResults = userSuggestionMaxSearchResults;
        cachedSuggestions = result;

        return result;
    }

    // XXX: needs optimisation
    public Map<String, Object> getPrefixedUserInfo(String id) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put(PREFIXED_ID_KEY_NAME, id);
        if (!StringUtils.isBlank(id)) {
            if (id.startsWith(NuxeoPrincipal.PREFIX)) {
                res.put(TYPE_KEY_NAME, USER_TYPE);
                String username = id.substring(NuxeoPrincipal.PREFIX.length());
                res.put(ID_KEY_NAME, username);
                res.put(ENTRY_KEY_NAME, userManager.getUserModel(username));
            } else if (id.startsWith(NuxeoGroup.PREFIX)) {
                res.put(TYPE_KEY_NAME, GROUP_TYPE);
                String groupname = id.substring(NuxeoGroup.PREFIX.length());
                res.put(ID_KEY_NAME, groupname);
                res.put(ENTRY_KEY_NAME, userManager.getGroupModel(groupname));
            } else {
                res.putAll(getUserInfo(id));
            }
        }
        return res;
    }

    // XXX: needs optimisation
    public Map<String, Object> getUserInfo(String id) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put(ID_KEY_NAME, id);
        if (!StringUtils.isBlank(id)) {
            if (userManager.getGroup(id) != null) {
                // group
                res.put(PREFIXED_ID_KEY_NAME, NuxeoGroup.PREFIX + id);
                res.put(TYPE_KEY_NAME, GROUP_TYPE);
                res.put(ENTRY_KEY_NAME, userManager.getGroupModel(id));
            } else {
                // user
                res.put(PREFIXED_ID_KEY_NAME, NuxeoPrincipal.PREFIX + id);
                res.put(TYPE_KEY_NAME, USER_TYPE);
                res.put(ENTRY_KEY_NAME, userManager.getUserModel(id));
            }
        }
        return res;
    }

    public Converter getUserConverter() {
        return new UserDisplayConverter();
    }

}
