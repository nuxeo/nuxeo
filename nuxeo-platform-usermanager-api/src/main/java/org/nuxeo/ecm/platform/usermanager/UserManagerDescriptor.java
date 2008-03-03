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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "userManager", order = { "users/anonymousUser",
        "users/anonymousUser@id", "users/anonymousUser/property" })
public class UserManagerDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<?> userManagerClass;

    @XNode("defaultGroup")
    protected String defaultGroup;

    @XNode("defaultAdministratorId")
    protected String rootLogin;

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

    protected boolean userSearchFieldsPresent;

    @XNode("users/searchFields")
    protected void setUserSearchFieldsPresent(
            @SuppressWarnings("unused")
    String text) {
        userSearchFieldsPresent = true;
    }

    @XNode("users/searchFields@append")
    protected boolean userSearchFieldsAppend;

    @XNodeList(value = "users/searchFields/searchField", type = HashSet.class, componentType = String.class)
    Set<String> userSearchFields;

    protected Pattern userPasswordPattern;

    @XNode("userPasswordPattern")
    protected void setUserPasswordPattern(String pattern) {
        userPasswordPattern = Pattern.compile(pattern);
    }

    protected boolean anonymousUserSpecified;

    @XNode("users/anonymousUser")
    protected void setAnonymousUserSpecified(@SuppressWarnings("unused")
    String text) {
        anonymousUserSpecified = true;
    }

    @XNode("users/anonymousUser@remove")
    protected boolean anonymousUserRemove;

    @XNode("users/anonymousUser@id")
    protected String anonymousUserId;

    protected Map<String, String> anonymousUser;

    @XNodeMap(value = "users/anonymousUser/property", key = "@name", type = HashMap.class, componentType = String.class)
    protected void setAnonymousUser(Map<String, String> properties) {
        // anonymousUserSpecified is already initialized because of the order
        // parameter in @XObject
        if (anonymousUserSpecified) {
            anonymousUser = properties;
            if (anonymousUserId != null) {
                // we should really use a dedicated class and not map
                anonymousUser.put(UserManager.ANONYMOUS_USER_ID_KEY,
                        anonymousUserId);
            }
        } else {
            // we have to do this hack because a XNodeMap is initialized to an
            // empty map even if the XML is not present
            anonymousUser = null;
        }
    }

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
        if (other.rootLogin != null) {
            rootLogin = other.rootLogin;
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
                userSearchFields.addAll(other.userSearchFields);
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
            if (other.anonymousUserRemove) {
                anonymousUser = null;
            } else {
                anonymousUser = other.anonymousUser;
            }
        }
    }

}
