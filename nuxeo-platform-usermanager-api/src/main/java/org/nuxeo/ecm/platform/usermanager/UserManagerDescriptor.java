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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.usermanager.UserManager.MatchType;

@XObject(value = "userManager")
public class UserManagerDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<?> userManagerClass;

    @XNode("defaultGroup")
    protected String defaultGroup;

    @XNodeList(value = "defaultAdministratorId", type = ArrayList.class, componentType = String.class)
    protected List<String> defaultAdministratorIds;

    @XNodeList(value = "administratorsGroup", type = ArrayList.class, componentType = String.class)
    protected List<String> administratorsGroups;

    @XNode("disableDefaultAdministratorsGroup")
    Boolean disableDefaultAdministratorsGroup;

    @XNode("userSortField")
    protected String userSortField;

    @XNode("groupSortField")
    protected String groupSortField;

    @XNode("users/directory")
    protected String userDirectoryName;

    @XNode("users/emailField")
    protected String userEmailField;

    @XNode("users/listingMode")
    protected String userListingMode;

    // BBB old syntax
    @XNode("userListingMode")
    protected void setUserListingMode(String userListingMode) {
        this.userListingMode = userListingMode;
    }

    protected boolean userSearchFieldsPresent = false;

    @XNode("users/searchFields")
    protected void setUserSearchFieldsPresent(@SuppressWarnings("unused")
    String text) {
        userSearchFieldsPresent = true;
    }

    @XNode("users/searchFields@append")
    protected boolean userSearchFieldsAppend;

    Map<String, MatchType> userSearchFields = new LinkedHashMap<String, MatchType>();

    @XNodeList(value = "users/searchFields/exactMatchSearchField", componentType = String.class, type = String[].class)
    protected void setExactMatchUserSearchFields(String[] fields) {
        for (String field : fields) {
            userSearchFields.put(field, MatchType.EXACT);
        }
    }

    @XNodeList(value = "users/searchFields/substringMatchSearchField", componentType = String.class, type = String[].class)
    protected void setSubstringMatchUserSearchFields(String[] fields) {
        for (String field : fields) {
            userSearchFields.put(field, MatchType.SUBSTRING);
        }
    }

    /**
     * @deprecated use setSubstringMatchUserSearchFields instead
     */
    @Deprecated
    @XNodeList(value = "users/searchFields/searchField", componentType = String.class, type = String[].class)
    protected void setUserSearchFields(String[] fields) {
        setSubstringMatchUserSearchFields(fields);
    }

    protected Pattern userPasswordPattern;

    @XNode("userPasswordPattern")
    protected void setUserPasswordPattern(String pattern) {
        userPasswordPattern = Pattern.compile(pattern);
    }

    @XNode("users/anonymousUser")
    protected VirtualUserDescriptor anonymousUser;

    @XNodeMap(value = "users/virtualUser", key = "@id", type = HashMap.class, componentType = VirtualUserDescriptor.class)
    protected Map<String, VirtualUserDescriptor> virtualUsers;

    @XNode("groups/directory")
    protected String groupDirectoryName;

    @XNode("groups/membersField")
    protected String groupMembersField;

    @XNode("groups/subGroupsField")
    protected String groupSubGroupsField;

    @XNode("groups/parentGroupsField")
    protected String groupParentGroupsField;

    @XNode("groups/listingMode")
    protected String groupListingMode;

    /**
     * Merge with data from another descriptor.
     */
    public void merge(UserManagerDescriptor other) {
        if (other.userManagerClass != null) {
            userManagerClass = other.userManagerClass;
        }
        if (other.userListingMode != null) {
            userListingMode = other.userListingMode;
        }
        if (other.groupListingMode != null) {
            groupListingMode = other.groupListingMode;
        }
        if (other.defaultGroup != null) {
            defaultGroup = other.defaultGroup;
        }
        if (other.defaultAdministratorIds != null) {
            if (defaultAdministratorIds == null) {
                defaultAdministratorIds = new ArrayList<String>();
            }
            defaultAdministratorIds.addAll(other.defaultAdministratorIds);
        }
        if (other.administratorsGroups != null) {
            if (administratorsGroups == null) {
                administratorsGroups = new ArrayList<String>();
            }
            administratorsGroups.addAll(other.administratorsGroups);
        }
        if (other.disableDefaultAdministratorsGroup != null) {
            disableDefaultAdministratorsGroup = other.disableDefaultAdministratorsGroup;
        }
        if (other.userSearchFieldsPresent) {
            if (other.userSearchFieldsAppend) {
                userSearchFields.putAll(other.userSearchFields);
            } else {
                userSearchFields = other.userSearchFields;
            }
        }

        if (other.userSortField != null) {
            userSortField = other.userSortField;
        }
        if (other.groupSortField != null) {
            groupSortField = other.groupSortField;
        }
        if (other.userDirectoryName != null) {
            userDirectoryName = other.userDirectoryName;
        }
        if (other.userEmailField != null) {
            userEmailField = other.userEmailField;
        }
        if (other.userSearchFieldsPresent) {
            if (other.userSearchFieldsAppend) {
                userSearchFields.putAll(other.userSearchFields);
            } else {
                userSearchFields = other.userSearchFields;
            }
        }
        if (other.userPasswordPattern != null) {
            userPasswordPattern = other.userPasswordPattern;
        }
        if (other.groupDirectoryName != null) {
            groupDirectoryName = other.groupDirectoryName;
        }
        if (other.groupMembersField != null) {
            groupMembersField = other.groupMembersField;
        }
        if (other.groupSubGroupsField != null) {
            groupSubGroupsField = other.groupSubGroupsField;
        }
        if (other.groupParentGroupsField != null) {
            groupParentGroupsField = other.groupParentGroupsField;
        }
        if (other.anonymousUser != null) {
            if (other.anonymousUser.remove) {
                anonymousUser = null;
            } else {
                anonymousUser = other.anonymousUser;
            }
        }
        if (other.virtualUsers != null) {
            if (virtualUsers == null) {
                virtualUsers = other.virtualUsers;
            } else {
                for (VirtualUserDescriptor otherVirtualUser : other.virtualUsers.values()) {
                    if (virtualUsers.containsKey(otherVirtualUser.id)
                            && otherVirtualUser.remove) {
                        virtualUsers.remove(otherVirtualUser.id);
                    } else {
                        virtualUsers.put(otherVirtualUser.id, otherVirtualUser);
                    }
                }
            }
        }
    }

}
