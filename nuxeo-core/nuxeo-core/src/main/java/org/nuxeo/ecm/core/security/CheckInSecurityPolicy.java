/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.security;

import java.security.Principal;
import java.util.Arrays;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Security policy that denies write access on a live document when it is in the checked-in state.
 * <p>
 * The document must be checked out before modification is allowed.
 *
 * @since 5.4
 */
public class CheckInSecurityPolicy extends AbstractSecurityPolicy {

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        if (Arrays.asList(resolvedPermissions).contains(SecurityConstants.WRITE_PROPERTIES) && !doc.isVersion()
                && !doc.isProxy()) {
            if (!doc.isCheckedOut()) {
                access = Access.DENY;
            }
        }
        return access;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        return permission.equals(SecurityConstants.WRITE);
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
