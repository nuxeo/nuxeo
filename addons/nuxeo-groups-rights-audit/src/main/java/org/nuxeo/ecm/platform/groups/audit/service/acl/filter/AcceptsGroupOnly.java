/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.filter;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Ignore users and keep groups. Warning: this filter assumes a {@link UserManager} is available.
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class AcceptsGroupOnly extends AbstractContentFilter implements IContentFilter {
    protected static Log log = LogFactory.getLog(AcceptsGroupOnly.class);

    protected UserManager um = Framework.getService(UserManager.class);

    protected Collection<String> groups = null;

    /**
     * Return true if the input string is the name of a group known by the {@link UserManager} service. Return also true
     * if input is equal to {@link SecurityConstants.EVERYONE}, since this is a special user name intended to define
     * inheritance blocker rules.
     */
    @Override
    public boolean acceptsUserOrGroup(String userOrGroup) {
        if (isEveryone(userOrGroup))
            return true;
        return isGroup(userOrGroup);
    }

    public boolean isGroup(String user) {
        if (groups == null)
            groups = um.getGroupIds();
        return groups.contains(user);
    }
}
