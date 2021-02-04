/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.usermanager;

import java.util.HashMap;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.nuxeo.ecm.platform.usermanager.UserManager.MatchType;
import org.w3c.dom.Element;

/**
 * Registry for custom merge logic on {@link UserManagerDescriptor}.
 *
 * @since 11.5
 */
public class UserManagerRegistry extends SingleRegistry {

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        if (existing == null) {
            existing = getInitialDescriptor();
        }
        ((UserManagerDescriptor) existing).merge(getInstance(ctx, xObject, element));
        return (T) existing;
    }

    protected UserManagerDescriptor getInitialDescriptor() {
        UserManagerDescriptor desc = new UserManagerDescriptor();
        desc.userListingMode = "search_only";
        // backward compatibility defaults
        desc.userDirectoryName = "userDirectory";
        desc.userEmailField = "email";

        desc.userSearchFields = new HashMap<>();
        desc.userSearchFields.put("username", MatchType.SUBSTRING);
        desc.userSearchFields.put("firstName", MatchType.SUBSTRING);
        desc.userSearchFields.put("lastName", MatchType.SUBSTRING);

        desc.groupDirectoryName = "groupDirectory";
        desc.groupLabelField = "grouplabel";
        desc.groupMembersField = "members";
        desc.groupSubGroupsField = "subGroups";
        desc.groupParentGroupsField = "parentGroups";

        desc.groupSearchFields = new HashMap<>();
        desc.groupSearchFields.put("groupname", MatchType.SUBSTRING);
        desc.groupSearchFields.put("grouplabel", MatchType.SUBSTRING);
        return desc;
    }

}
