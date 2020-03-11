/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;

/**
 * Security policy used for tests that grants permissions depending on the user access level and confidentiality set on
 * the document.
 *
 * @author Anahide Tchertchian
 */
public class AccessLevelSecurityPolicy extends AbstractSecurityPolicy {

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, NuxeoPrincipal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        try {
            if ("Folder".equals(doc.getType().getName())) {
                DocumentModel userModel = principal.getModel();
                if (userModel != null) {
                    Long accessLevel = (Long) userModel.getPropertyValue("user:accessLevel");
                    Long securityLevel = (Long) doc.getPropertyValue("sp:securityLevel");
                    if (accessLevel.longValue() >= securityLevel.longValue()) {
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
