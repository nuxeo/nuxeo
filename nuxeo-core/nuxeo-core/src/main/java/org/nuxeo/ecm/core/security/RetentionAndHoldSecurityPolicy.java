/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.Arrays;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Security policy that prevents deletion of a document when it is under retention or has a legal hold.
 *
 * @since 11.1
 */
public class RetentionAndHoldSecurityPolicy extends AbstractSecurityPolicy {

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, NuxeoPrincipal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        if (!Arrays.asList(resolvedPermissions).contains(SecurityConstants.REMOVE)) {
            // not checking REMOVE, ignore
            return Access.UNKNOWN;
        }
        if (!doc.isUnderRetentionOrLegalHold()) {
            return Access.UNKNOWN;
        }
        return Access.DENY;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        // the important aspect is that we don't restrict BROWSE
        return permission.equals(SecurityConstants.REMOVE);
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
