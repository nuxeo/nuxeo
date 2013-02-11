/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.filter;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Ignore users and keep groups.
 *
 * Warning: this filter assumes a {@link UserManager} is available.
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 *
 */
public class AcceptsGroupOnly extends AbstractContentFilter implements
        IContentFilter {
    protected static Log log = LogFactory.getLog(AcceptsGroupOnly.class);

    protected UserManager um = Framework.getLocalService(UserManager.class);

    protected Collection<String> groups = null;

    /**
     * Return true if the input string is the name of a group known by the
     * {@link UserManager} service.
     *
     * Return also true if input is equal to {@link SecurityConstants.EVERYONE},
     * since this is a special user name intended to define inheritance blocker
     * rules.
     */
    @Override
    public boolean acceptsUserOrGroup(String userOrGroup) {
        if (isEveryone(userOrGroup))
            return true;

        try {
            final boolean s = isGroup(userOrGroup);
            return s;
        } catch (ClientException e) {
            log.error(e, e);
            return false;
        }
    }

    public boolean isGroup(String user) throws ClientException {
        if (groups == null)
            groups = um.getGroupIds();
        return groups.contains(user);
    }
}
