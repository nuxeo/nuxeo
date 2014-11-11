/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.document.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.service.extensions.WorkflowDocumentSecurityPolicyDescriptor;
import org.nuxeo.ecm.platform.workflow.document.service.extensions.WorkflowDocumentSecurityPolicyRelationDescriptor;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Workflow documents rights policy service
 * <p>
 * Deal with the registration of document rights policies.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkflowDocumentSecurityPolicyService extends DefaultComponent
        implements WorkflowDocumentSecurityPolicyManager {

    private static final long serialVersionUID = 6097407199815945362L;

    private static final Log log = LogFactory.getLog(WorkflowRulesService.class);

    /** Map from policy name -> policy instance. */
    protected final Map<String, WorkflowDocumentSecurityPolicy> wDocRightsPolicyMap;

    /** Map from workflow name -> policy name. */
    protected final Map<String, String> workflowPolicyMap;

    public WorkflowDocumentSecurityPolicyService() {
        wDocRightsPolicyMap = new HashMap<String, WorkflowDocumentSecurityPolicy>();
        workflowPolicyMap = new HashMap<String, String>();
    }

    public WorkflowDocumentSecurityPolicy getWorkflowDocumentSecurityPolicyByName(
            String name) {
        return wDocRightsPolicyMap.get(name);
    }

    public WorkflowDocumentSecurityPolicy getWorkflowDocumentSecurityPolicyFor(
            String workflowName) {
        String policyName = workflowPolicyMap.get(workflowName);
        return getWorkflowDocumentSecurityPolicyByName(policyName);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(
                    "workflowDocumentSecurityPolicy")) {
                for (Object contribution : contributions) {
                    WorkflowDocumentSecurityPolicyDescriptor desc = (WorkflowDocumentSecurityPolicyDescriptor) contribution;
                    if (desc.getName() != null) {
                        WorkflowDocumentSecurityPolicy policy = (WorkflowDocumentSecurityPolicy) extension.getContext()
                                .loadClass(desc.getClassName()).newInstance();
                        policy.setName(desc.getName());
                        wDocRightsPolicyMap.put(desc.getName(), policy);
                        log.info("Register new document rights policy extensions with name="
                                        + desc.getName());

                    }
                }
            }
            if (extension.getExtensionPoint().equals(
                    "workflowDocumentSecurityPolicyRelation")) {
                for (Object contribution : contributions) {
                    WorkflowDocumentSecurityPolicyRelationDescriptor desc = (WorkflowDocumentSecurityPolicyRelationDescriptor) contribution;
                    List<String> workflowNames = desc.getWorkflowNames();
                    for (String workflowName : workflowNames) {
                        workflowPolicyMap.put(workflowName,
                                desc.getPolicyName());
                    }
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(
                    "workflowDocumentSecurityPolicy")) {
                for (Object contribution : contributions) {
                    WorkflowDocumentSecurityPolicyDescriptor desc = (WorkflowDocumentSecurityPolicyDescriptor) contribution;
                    log.info("Unregister document rights policy extensions with name="
                                    + desc.getName());
                    wDocRightsPolicyMap.remove(desc.getName());
                }
            }
        }
        if (extension.getExtensionPoint().equals(
                "workflowDocumentSecurityPolicyRelation")) {
            for (Object contribution : contributions) {
                WorkflowDocumentSecurityPolicyRelationDescriptor desc = (WorkflowDocumentSecurityPolicyRelationDescriptor) contribution;
                for (String workflowName : desc.getWorkflowNames()) {
                    if (workflowPolicyMap.get(workflowName).equals(
                            desc.getPolicyName())) {
                        workflowPolicyMap.remove(workflowName);
                    }
                }
            }
        }
    }

}
