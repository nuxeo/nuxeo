/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.core.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Security policy service implementation.
 * <p>
 * Iterates over ordered policies. First policy to give a known access (grant or deny) applies.
 */
public class SecurityPolicyServiceImpl implements SecurityPolicyService {

    private static final Logger log = LogManager.getLogger(SecurityPolicyServiceImpl.class);

    private final List<SecurityPolicy> policies = new ArrayList<>();

    /**
     * Service initialized with contributed policy descriptors.
     *
     * @since 11.5
     */
    public SecurityPolicyServiceImpl(List<SecurityPolicyDescriptor> policyDescriptors) {
        List<SecurityPolicyDescriptor> orderedDescriptors = new ArrayList<>(policyDescriptors);
        Collections.sort(orderedDescriptors);
        List<String> policyNames = new ArrayList<>();
        for (SecurityPolicyDescriptor descriptor : orderedDescriptors) {
            try {
                Object policy = descriptor.getPolicy().getDeclaredConstructor().newInstance();
                if (policy instanceof SecurityPolicy) {
                    policies.add((SecurityPolicy) policy);
                    policyNames.add(descriptor.getName());
                } else {
                    log.error(
                            "Invalid contribution to security policy service {}: class {} "
                                    + "must implement the SecurityPolicy interface",
                            descriptor::getName, descriptor::getPolicy);
                }
            } catch (ReflectiveOperationException e) {
                log.error(e, e);
            }
        }
        log.debug("Ordered security policies: {}", policyNames);
    }

    @Override
    public List<SecurityPolicy> getPolicies() {
        return Collections.unmodifiableList(policies);
    }

    @Override
    public boolean arePoliciesRestrictingPermission(String permission) {
        for (SecurityPolicy policy : getPolicies()) {
            if (policy.isRestrictingPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean arePoliciesExpressibleInQuery(String repositoryName) {
        for (SecurityPolicy policy : getPolicies()) {
            if (!policy.isExpressibleInQuery(repositoryName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Collection<SQLQuery.Transformer> getPoliciesQueryTransformers(String repositoryName) {
        List<SQLQuery.Transformer> transformers = new LinkedList<>();
        for (SecurityPolicy policy : getPolicies()) {
            if (policy.isExpressibleInQuery(repositoryName)) {
                transformers.add(policy.getQueryTransformer(repositoryName));
            } else {
                log.warn("Security policy '%s' for repository '%s' cannot be expressed in SQL query.",
                        policy.getClass().getName(), repositoryName);
            }
        }
        return transformers;
    }

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, NuxeoPrincipal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        for (SecurityPolicy policy : getPolicies()) {
            Access policyAccess = policy.checkPermission(doc, mergedAcp, principal, permission, resolvedPermissions,
                    additionalPrincipals);
            if (policyAccess != null && !Access.UNKNOWN.equals(policyAccess)) {
                access = policyAccess;
                break;
            }
        }
        return access;
    }

}
