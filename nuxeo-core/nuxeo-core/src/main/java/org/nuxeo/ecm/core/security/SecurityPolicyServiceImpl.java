/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Security policy service implementation.
 * <p>
 * Iterates over ordered policies. First policy to give a known access (grant
 * or deny) applies.
 *
 * @author Anahide Tchertchian
 */
public class SecurityPolicyServiceImpl implements SecurityPolicyService {

    private static final long serialVersionUID = 482814921906794786L;

    private static final Log log = LogFactory.getLog(SecurityPolicyServiceImpl.class);

    private final Map<String, SecurityPolicyDescriptor> policyDescriptors;

    private List<SecurityPolicy> policies;

    public SecurityPolicyServiceImpl() {
        policyDescriptors = new Hashtable<String, SecurityPolicyDescriptor>();
    }

    private void computePolicies() {
        policies = new ArrayList<SecurityPolicy>();
        List<SecurityPolicyDescriptor> orderedDescriptors = new ArrayList<SecurityPolicyDescriptor>();
        for (SecurityPolicyDescriptor descriptor : policyDescriptors.values()) {
            if (descriptor.isEnabled()) {
                orderedDescriptors.add(descriptor);
            }
        }
        Collections.sort(orderedDescriptors);
        List<String> policyNames = new ArrayList<String>();
        for (SecurityPolicyDescriptor descriptor : orderedDescriptors) {
            if (descriptor.isEnabled()) {
                try {
                    Object policy = descriptor.getPolicy().newInstance();
                    if (policy instanceof SecurityPolicy) {
                        policies.add((SecurityPolicy) policy);
                        policyNames.add(descriptor.getName());
                    } else {
                        log.error(String.format(
                                "Invalid contribution to security policy service %s:"
                                        + " must implement SecurityPolicy interface",
                                descriptor.getName()));
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
        log.debug("Ordered security policies: " + policyNames.toString());
    }

    @Override
    public synchronized List<SecurityPolicy> getPolicies() {
        if (policies == null) {
            computePolicies();
        }
        return policies;
    }

    private void resetPolicies() {
        policies = null;
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
    public Collection<SQLQuery.Transformer> getPoliciesQueryTransformers(
            String repositoryName) {
        List<SQLQuery.Transformer> transformers = new LinkedList<SQLQuery.Transformer>();
        for (SecurityPolicy policy : getPolicies()) {
            if (policy.isExpressibleInQuery(repositoryName)) {
                transformers.add(policy.getQueryTransformer(repositoryName));
            }
        }
        return transformers;
    }

    @Override
    public void registerDescriptor(SecurityPolicyDescriptor descriptor) {
        String id = descriptor.getName();
        if (policyDescriptors.containsKey(id)) {
            log.info("Overriding security policy " + id);
        }
        policyDescriptors.put(id, descriptor);
        resetPolicies();
    }

    @Override
    public void unregisterDescriptor(SecurityPolicyDescriptor descriptor) {
        String id = descriptor.getName();
        if (policyDescriptors.containsKey(id)) {
            policyDescriptors.remove(id);
            resetPolicies();
        }
    }

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp,
            Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        List<SecurityPolicy> policies = getPolicies();
        for (SecurityPolicy policy : policies) {
            Access policyAccess = policy.checkPermission(doc, mergedAcp,
                    principal, permission, resolvedPermissions,
                    additionalPrincipals);
            if (policyAccess != null && !Access.UNKNOWN.equals(policyAccess)) {
                access = policyAccess;
                break;
            }
        }
        return access;
    }

}
