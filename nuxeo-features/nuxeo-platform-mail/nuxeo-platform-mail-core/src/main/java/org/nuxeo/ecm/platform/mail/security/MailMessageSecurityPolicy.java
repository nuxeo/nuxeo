/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *      André Justo
 */
package org.nuxeo.ecm.platform.mail.security;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MAIL_MESSAGE_TYPE;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;

/**
 * Security policy that denies Write access on MailMessage documents.
 *
 * @since 10.1
 */
public class MailMessageSecurityPolicy extends AbstractSecurityPolicy {

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, NuxeoPrincipal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        if (doc.getType().getName().equals(MAIL_MESSAGE_TYPE)) {
            List<String> resolvedPermissionsList = Arrays.asList(resolvedPermissions);
            if (resolvedPermissionsList.contains(SecurityConstants.WRITE_PROPERTIES)
                || resolvedPermissionsList.contains(SecurityConstants.WRITE)) {
                access = Access.DENY;
            }
        }
        return access;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        return SecurityConstants.WRITE.equals(permission) || SecurityConstants.WRITE_PROPERTIES.equals(permission);
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return true;
    }

    @Override
    public SQLQuery.Transformer getQueryTransformer(String repositoryName) {
        return SQLQuery.Transformer.IDENTITY;
    }
}
