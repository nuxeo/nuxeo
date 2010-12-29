/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import org.nuxeo.apidoc.security.SecurityConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebContext;

public class SecurityHelper {

    public static boolean canEditDocumentation(WebContext ctx) {
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        return canEditDocumentation(principal);
    }

    public static boolean canEditDocumentation(NuxeoPrincipal principal) {

        if (principal.isAdministrator()) {
            return true;
        }
        if (principal.isAnonymous()) {
            return false;
        }
        return principal.getAllGroups().contains(SecurityConstants.Write_Group);

    }

}
