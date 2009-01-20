/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
    public static final String USER_ICON = "/icons/user.gif";
    public static final String GROUP_ICON = "/icons/group.gif";
    public static final String USER_ICON_ALT = "user.gif";
    public static final String GROUP_ICON_ALT = "group.gif";

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

    public String getPrincipalType(String name) throws ClientException {
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

    protected List<NuxeoPrincipal> getSuggestedUsers() {
        if (searchFilter == null || searchFilter.length() == 0) {
            return Collections.emptyList();
        }

        List<NuxeoPrincipal> result;
        try {
            result = userManager.searchPrincipals(searchFilter);
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            return Collections.emptyList();
        } catch (ClientException e) {
            // XXX: this exception should not be catched
            log.error("error searching for principals: " + e.getMessage());
            return Collections.emptyList();
        }

        if (result.size() > MAX_SEARCH_RESULTS) {
            searchOverflow = true;
            return Collections.emptyList();
        }
        return result;
    }

    protected List<NuxeoGroup> getSuggestedGroups() {
        if (searchFilter == null || searchFilter.length() == 0) {
            return Collections.emptyList();
        }

        List<NuxeoGroup> result;
        try {
            result = userManager.searchGroups(searchFilter);
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            return Collections.emptyList();
        } catch (ClientException e) {
            log.error("error searching for groups: " + e.getMessage());
            return Collections.emptyList();
        }

        if (result.size() > MAX_SEARCH_RESULTS) {
            searchOverflow = true;
            return Collections.emptyList();
        }
        return result;
    }

    public List<Map<String, Object>> getSuggestedEntries() {
        if (searchFilter == null || searchFilter.length() == 0) {
            return Collections.emptyList();
        }
        if (searchFilter.equals(previousSearchFilter)
                && searchType.equals(previousSearchType)) {
            searchOverflow = previousSearchOverflow;
            return previousSuggestedEntries;
        }

        searchOverflow = false;

        List<NuxeoPrincipal> users;
        if (USER_TYPE.equals(searchType) || USER_GROUP_TYPE.equals(searchType)
                || StringUtils.isEmpty(searchType)) {
            users = getSuggestedUsers();
        } else {
            users = Collections.emptyList();
        }

        List<NuxeoGroup> groups;
        if (GROUP_TYPE.equals(searchType) || USER_GROUP_TYPE.equals(searchType)
                || StringUtils.isEmpty(searchType)) {
            groups = getSuggestedGroups();
        } else {
            groups = Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(
                users.size() + groups.size());

        for (NuxeoPrincipal user : users) {
            if (user == null) {
                continue;
            }

            String name = user.getName();
            StringBuilder label = new StringBuilder(name).append("  (");
            if (user.getFirstName() != null) {
                label.append(user.getFirstName());
            }
            if (user.getLastName() != null) {
                label.append(' ').append(user.getLastName());
            }
            label.append(')');

            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("label", label.toString());
            entry.put("id", name);
            entry.put("icon", "icons/user.gif");
            result.add(entry);
        }

        for (NuxeoGroup group : groups) {
            Map<String, Object> entry = new HashMap<String, Object>();
            String name = group.getName();
            entry.put("label", name);
            entry.put("id", name);
            entry.put("icon", "icons/group.gif");
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
