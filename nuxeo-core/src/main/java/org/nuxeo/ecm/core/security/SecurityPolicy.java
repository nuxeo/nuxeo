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

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;

/**
 * Interface for pluggable core security policy.
 *
 * @author Anahide Tchertchian
 */
public interface SecurityPolicy {

    /**
     * Checks given permission for doc and principal.
     *
     * @param doc the document to check
     * @param mergedAcp merged acp resolved for this document
     * @param principal principal to check
     * @param permission permission to check
     * @param resolvedPermissions permissions or groups of permissions
     *            containing permission
     * @param additionalPrincipals
     * @return access: true, false, or nothing. When nothing is returned,
     *         following policies or default core security are applied.
     * @throws SecurityException
     */
    Access checkPermission(Document doc, ACP mergedAcp, Principal principal,
            String permission, String[] resolvedPermissions,
            String[] additionalPrincipals) throws SecurityException;

}
