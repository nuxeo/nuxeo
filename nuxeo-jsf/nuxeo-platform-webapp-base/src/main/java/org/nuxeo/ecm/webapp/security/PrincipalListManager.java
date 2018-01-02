/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * POJO class that extracts and holds the list of the users from backend.
 *
 * @author Razvan Caraghin
 * @author <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */
@Name("principalListManager")
@Scope(SESSION)
public class PrincipalListManager implements Serializable {

    public static final String USER_TYPE = "USER_TYPE";

    public static final String GROUP_TYPE = "GROUP_TYPE";

    public static final String USER_GROUP_TYPE = "USER_GROUP_TYPE";

    public static final String USER_ICON = "/icons/user.png";

    public static final String GROUP_ICON = "/icons/group.png";

    public static final String USER_ICON_ALT = "user.png";

    public static final String GROUP_ICON_ALT = "group.png";

    public static final int MAX_SEARCH_RESULTS = 20;

    private static final long serialVersionUID = 1859670282887307916L;

    private static final Log log = LogFactory.getLog(PrincipalListManager.class);

    public final Map<String, String> iconPath;

    public final Map<String, String> iconAlt;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected SelectItem[] availablePrincipals;

    protected Map<String, String> principalTypes = new HashMap<String, String>();

    protected String selectedPrincipal;

    protected String searchType;

    protected List<String> selectedUsers;

    @In(required = false)
    String searchFilter;

    private boolean searchOverflow;

    // cache of previous results
    protected transient List<Map<String, Object>> previousSuggestedEntries;

    // previous search filter
    protected transient String previousSearchFilter;

    // previous search type
    protected transient String previousSearchType;

    // previous search overflow
    protected transient boolean previousSearchOverflow;

    public PrincipalListManager() {
        iconPath = new HashMap<String, String>();
        iconAlt = new HashMap<String, String>();
        iconPath.put(USER_TYPE, USER_ICON);
        iconPath.put(GROUP_TYPE, GROUP_ICON);

        iconAlt.put(USER_TYPE, USER_ICON_ALT);
        iconAlt.put(GROUP_TYPE, GROUP_ICON_ALT);
        searchType = USER_GROUP_TYPE;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        Context pageContext = Contexts.getPageContext();
        if (pageContext != null) {
            pageContext.set("searchFilter", searchFilter);
        }
        this.searchFilter = searchFilter;
    }

    public String getSelectedPrincipal() {
        return selectedPrincipal;
    }

    public String getPrincipalType(String name) {
        // happens when used in NXMethodResults in A4JCalls !!!
        if (name == null) {
            return null;
        }
        if (principalTypes == null) {
            principalTypes = new HashMap<String, String>();
        }
        String type = principalTypes.get(name);
        if (type == null) {
            if (userManager.getGroup(name) != null) {
                type = GROUP_TYPE;
            } else {
                type = USER_TYPE;
            }
            principalTypes.put(name, type);
        }
        return type;
    }

    public void setSelectedPrincipal(String selectedPrincipal) {
        this.selectedPrincipal = selectedPrincipal;
    }

    protected DocumentModelList getSuggestedUsers() {
        if (searchFilter == null || searchFilter.length() == 0) {
            return new DocumentModelListImpl();
        }

        DocumentModelList result;
        try {
            result = userManager.searchUsers(searchFilter);
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            return new DocumentModelListImpl();
        }

        if (result.size() > MAX_SEARCH_RESULTS) {
            searchOverflow = true;
            return new DocumentModelListImpl();
        }
        return result;
    }

    protected DocumentModelList getSuggestedGroups() {
        if (searchFilter == null || searchFilter.length() == 0) {
            return new DocumentModelListImpl();
        }

        DocumentModelList result;
        try {
            result = userManager.searchGroups(searchFilter);
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            return new DocumentModelListImpl();
        }

        if (result.size() > MAX_SEARCH_RESULTS) {
            searchOverflow = true;
            return new DocumentModelListImpl();
        }
        return result;
    }

    public List<Map<String, Object>> getSuggestedEntries() {
        if (searchFilter == null || searchFilter.length() == 0) {
            return Collections.emptyList();
        }
        if (searchFilter.equals(previousSearchFilter) && searchType.equals(previousSearchType)) {
            searchOverflow = previousSearchOverflow;
            return previousSuggestedEntries;
        }

        searchOverflow = false;

        DocumentModelList users;
        if (USER_TYPE.equals(searchType) || USER_GROUP_TYPE.equals(searchType) || StringUtils.isEmpty(searchType)) {
            users = getSuggestedUsers();
        } else {
            users = new DocumentModelListImpl();
        }

        DocumentModelList groups;
        if (GROUP_TYPE.equals(searchType) || USER_GROUP_TYPE.equals(searchType) || StringUtils.isEmpty(searchType)) {
            groups = getSuggestedGroups();
        } else {
            groups = new DocumentModelListImpl();
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(users.size() + groups.size());

        for (DocumentModel user : users) {
            if (user == null) {
                continue;
            }
            NuxeoPrincipal principal = userManager.getPrincipal(user.getId());
            String name = principal.getName();
            StringBuilder label = new StringBuilder(name).append("  (");
            if (principal.getFirstName() != null) {
                label.append(principal.getFirstName());
            }
            if (principal.getLastName() != null) {
                label.append(' ').append(principal.getLastName());
            }
            label.append(')');

            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("label", label.toString());
            entry.put("id", name);
            entry.put("icon", "icons/user.png");
            result.add(entry);
        }

        for (DocumentModel group : groups) {
            Map<String, Object> entry = new HashMap<String, Object>();
            try {
                entry.put("label",
                        group.getProperty(userManager.getGroupSchemaName(), userManager.getGroupLabelField()));
            } catch (PropertyException e) {
                log.warn("Unable to get group label of " + group.getId());
                log.debug(e);
                entry.put("label", group.getId());
            }
            entry.put("id", group.getId());
            entry.put("icon", "icons/group.png");
            result.add(entry);
        }

        // put in cache
        previousSuggestedEntries = result;
        previousSearchOverflow = searchOverflow;
        previousSearchType = searchType;
        previousSearchFilter = searchFilter;
        return result;
    }

    public boolean getDisplaySearchResults() {
        return searchFilter != null && searchFilter.length() != 0;
    }

    public void resetSearchFilter() {
        searchFilter = null;
    }

    public String addToSelectedUsers(String userName) {
        if (selectedUsers == null) {
            selectedUsers = new ArrayList<String>();
        }

        if (!selectedUsers.contains(userName)) {
            selectedUsers.add(userName);
        }
        return null;
    }

    public String removeFromSelectedUsers(String userName) {
        if (selectedUsers == null) {
            selectedUsers = new ArrayList<String>();
        }

        if (selectedUsers.contains(userName)) {
            selectedUsers.remove(userName);
        }
        return null;
    }

    public List<String> getSelectedUsers() {
        if (selectedUsers == null) {
            return new ArrayList<String>();
        }
        return selectedUsers;
    }

    public void setSelectedUsers(List<String> selectedUsers) {
        this.selectedUsers = selectedUsers;
    }

    public boolean getSelectedUserListEmpty() {
        return selectedUsers == null || selectedUsers.isEmpty();
    }

    public void resetSelectedUserList() {
        selectedUsers = null;
    }

    public boolean getSearchOverflow() {
        return searchOverflow;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        if (searchType == null) {
            searchType = USER_GROUP_TYPE;
        }
        this.searchType = searchType;
    }

}
