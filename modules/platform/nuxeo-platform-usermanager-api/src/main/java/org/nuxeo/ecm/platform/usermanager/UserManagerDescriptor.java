/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.usermanager;

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

/**
 * APG-240 All attributes are defined public because the user manager service do not get access to the fields. OSGI
 * don't allow splitted packages having access to public members defined from an another package provider.
 *
 * @author matic
 */
@XObject(value = "userManager")
public class UserManagerDescriptor {

    @XNode("@class")
    public Class<?> userManagerClass;

    @XNode("defaultGroup")
    public String defaultGroup;

    @XNodeList(value = "defaultAdministratorId", type = ArrayList.class, componentType = String.class)
    public List<String> defaultAdministratorIds;

    @XNodeList(value = "administratorsGroup", type = ArrayList.class, componentType = String.class)
    public List<String> administratorsGroups;

    @XNode("disableDefaultAdministratorsGroup")
    public Boolean disableDefaultAdministratorsGroup;

    @XNode("userSortField")
    public String userSortField;

    @XNode("groupSortField")
    public String groupSortField;

    @XNode("users/directory")
    public String userDirectoryName;

    @XNode("users/emailField")
    public String userEmailField;

    @XNode("users/listingMode")
    public String userListingMode;

    // BBB old syntax
    @XNode("userListingMode")
    public void setUserListingMode(String userListingMode) {
        this.userListingMode = userListingMode;
    }

    public boolean userSearchFieldsPresent = false;

    @XNode("users/searchFields")
    public void setUserSearchFieldsPresent(@SuppressWarnings("unused") String text) {
        userSearchFieldsPresent = true;
    }

    @XNode("users/searchFields@append")
    public boolean userSearchFieldsAppend;

    public Map<String, MatchType> userSearchFields = new LinkedHashMap<>();

    @XNodeList(value = "users/searchFields/exactMatchSearchField", componentType = String.class, type = String[].class)
    public void setExactMatchUserSearchFields(String[] fields) {
        for (String field : fields) {
            userSearchFields.put(field, MatchType.EXACT);
        }
    }

    @XNodeList(value = "users/searchFields/substringMatchSearchField", componentType = String.class, type = String[].class)
    public void setSubstringMatchUserSearchFields(String[] fields) {
        for (String field : fields) {
            userSearchFields.put(field, MatchType.SUBSTRING);
        }
    }

    public Pattern userPasswordPattern;

    @XNode("userPasswordPattern")
    public void setUserPasswordPattern(String pattern) {
        userPasswordPattern = Pattern.compile(pattern);
    }

    @XNode("users/anonymousUser")
    public VirtualUserDescriptor anonymousUser;

    @XNodeMap(value = "users/virtualUser", key = "@id", type = HashMap.class, componentType = VirtualUserDescriptor.class)
    public Map<String, VirtualUserDescriptor> virtualUsers;

    @XNode("groups/directory")
    public String groupDirectoryName;

    @XNode("groups/groupLabelField")
    public String groupLabelField;

    @XNode("groups/membersField")
    public String groupMembersField;

    @XNode("groups/subGroupsField")
    public String groupSubGroupsField;

    @XNode("groups/parentGroupsField")
    public String groupParentGroupsField;

    @XNode("groups/listingMode")
    public String groupListingMode;

    public boolean groupSearchFieldsPresent = false;

    @XNode("groups/searchFields")
    public void setGroupSearchFieldsPresent(@SuppressWarnings("unused") String text) {
        groupSearchFieldsPresent = true;
    }

    @XNode("groups/searchFields@append")
    public boolean groupSearchFieldsAppend;

    public Map<String, MatchType> groupSearchFields = new LinkedHashMap<>();

    @XNodeList(value = "groups/searchFields/exactMatchSearchField", componentType = String.class, type = String[].class)
    public void setExactMatchGroupSearchFields(String[] fields) {
        for (String field : fields) {
            groupSearchFields.put(field, MatchType.EXACT);
        }
    }

    @XNodeList(value = "groups/searchFields/substringMatchSearchField", componentType = String.class, type = String[].class)
    public void setSubstringMatchGroupSearchFields(String[] fields) {
        for (String field : fields) {
            groupSearchFields.put(field, MatchType.SUBSTRING);
        }
    }

    @XNode("digestAuthDirectory")
    public String digestAuthDirectory;

    @XNode("digestAuthRealm")
    public String digestAuthRealm;

    @XNode("userCacheName")
    public String userCacheName;

    /**
     * Merge with data from another descriptor.
     */
    public void merge(UserManagerDescriptor other) {
        if (other.userManagerClass != null) {
            userManagerClass = other.userManagerClass;
        }
        if (other.userCacheName != null) {
            userCacheName = other.userCacheName;
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
                defaultAdministratorIds = new ArrayList<>();
            }
            defaultAdministratorIds.addAll(other.defaultAdministratorIds);
        }
        if (other.administratorsGroups != null) {
            if (administratorsGroups == null) {
                administratorsGroups = new ArrayList<>();
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
        if (other.groupLabelField != null) {
            groupLabelField = other.groupLabelField;
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
        if (other.groupSearchFieldsPresent) {
            if (other.groupSearchFieldsAppend) {
                groupSearchFields.putAll(other.groupSearchFields);
            } else {
                groupSearchFields = other.groupSearchFields;
            }
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
                    if (virtualUsers.containsKey(otherVirtualUser.id) && otherVirtualUser.remove) {
                        virtualUsers.remove(otherVirtualUser.id);
                    } else {
                        virtualUsers.put(otherVirtualUser.id, otherVirtualUser);
                    }
                }
            }
        }
        if (other.digestAuthDirectory != null) {
            digestAuthDirectory = other.digestAuthDirectory;
        }
        if (other.digestAuthRealm != null) {
            digestAuthRealm = other.digestAuthRealm;
        }
    }

}
