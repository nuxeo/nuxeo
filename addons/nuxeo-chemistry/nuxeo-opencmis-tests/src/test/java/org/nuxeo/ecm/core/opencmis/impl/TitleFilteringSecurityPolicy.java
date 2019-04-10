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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.security.Principal;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;

/**
 * Test security policy that forbids titles starting with SECRET.
 *
 * @since 5.7.2
 */
public class TitleFilteringSecurityPolicy extends AbstractSecurityPolicy {

    protected static final String PREFIX = "SECRET";

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp,
            Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        if (!isRestrictingPermission(permission)) {
            return Access.UNKNOWN;
        }
        String title;
        try {
            title = (String) doc.getPropertyValue("dc:title");
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        if (title != null && title.startsWith(PREFIX)) {
            return Access.DENY;
        }
        return Access.UNKNOWN;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        return permission.equals(SecurityConstants.BROWSE);
    }

    @Override
    public boolean isExpressibleInQuery() {
        return false;
    }

}
