/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import java.security.Principal;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;

/**
 * Security policy used for tests that grants permissions depending on the user
 * access level and confidentiality set on the document.
 *
 * @author Anahide Tchertchian
 *
 */
public class AccessLevelSecurityPolicy extends AbstractSecurityPolicy {

    public Access checkPermission(Document doc, ACP mergedAcp,
            Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        try {
            if ("Folder".equals(doc.getType().getName())
                    && principal instanceof NuxeoPrincipal) {
                DocumentModel userModel = ((NuxeoPrincipal) principal).getModel();
                if (userModel != null) {
                    Long accessLevel = (Long) userModel.getPropertyValue("user:accessLevel");
                    Long securityLevel = (Long) doc.getPropertyValue("sp:securityLevel");
                    if (accessLevel >= securityLevel) {
                        access = Access.GRANT;
                    } else {
                        access = Access.DENY;
                    }
                }
            }
        } catch (Exception e) {
        }
        return access;
    }

}
