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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
import org.nuxeo.ecm.platform.workflow.document.service.extensions.WorkflowDoctypeRuleDescriptor;
import org.nuxeo.ecm.platform.workflow.document.service.extensions.WorkflowPathRuleDescriptor;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Workflow rules implementation.
 * <p>
 * :XXX: plug me on NXRules
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>*
 */
public class WorkflowRulesService extends DefaultComponent implements
        WorkflowRulesManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.workflow.document.service.WorkflowRulesService");

    private static final long serialVersionUID = -2935604737752201932L;

    private static final Log log = LogFactory.getLog(WorkflowRulesService.class);

    /** Rules: doctype -> workflow definition ids. */
    final Map<String, Set<String>> docTypeDefinitionsRulesMap = new HashMap<String, Set<String>>();

    /** Rules: path -> workflow definition ids. */
    final Map<String, Set<String>> pathDefinitionsRulesMap = new HashMap<String, Set<String>>();

    public Set<String> getAllowedWorkflowDefinitionNamesByDoctype(String docType) {
        Set<String> defs;
        if (docTypeDefinitionsRulesMap.containsKey(docType)) {
            defs = docTypeDefinitionsRulesMap.get(docType);
        } else {
            defs = new LinkedHashSet<String>();
        }
        return defs;
    }

    public Set<String> getAllowedWorkflowDefinitionNamesByPath(String path) {
        Set<String> defs = new LinkedHashSet<String>();
        // :XXX: Deal with override
        Set<String> keys = pathDefinitionsRulesMap.keySet();
        for (String key : keys) {
            // :TODO: Check if we want lower case match
            if (path.toLowerCase().startsWith(key.toLowerCase())) {
                defs.addAll(pathDefinitionsRulesMap.get(key));
            }
        }
        return defs;
    }

    public Set<String> getAllowedWorkflowDefinitionNames(String path,
            String docType) {
        Set<String> s1 = getAllowedWorkflowDefinitionNamesByDoctype(docType);
        Set<String> s2 = getAllowedWorkflowDefinitionNamesByPath(path);
        s1.retainAll(s2);
        return s1;
    }

    public void addRuleByType(String workflowDefinitionId, String docType) {
        Set<String> defs;
        if (docTypeDefinitionsRulesMap.containsKey(docType)) {
            defs = docTypeDefinitionsRulesMap.get(docType);
        } else {
            defs = new LinkedHashSet<String>();
        }
        defs.add(workflowDefinitionId);
        docTypeDefinitionsRulesMap.put(docType, defs);
    }

    public void addRuleByPath(String workflowInstanceId, String path) {
        Set<String> defs;
        if (pathDefinitionsRulesMap.containsKey(path)) {
            defs = pathDefinitionsRulesMap.get(path);
        } else {
            defs = new LinkedHashSet<String>();
        }

        defs.add(workflowInstanceId);
        pathDefinitionsRulesMap.put(path, defs);
    }

    public void delRuleByType(String workflowDefinitionId, String docType) {
        if (getAllowedWorkflowDefinitionNamesByDoctype(docType).contains(
                workflowDefinitionId)) {
            Set<String> defs = getAllowedWorkflowDefinitionNamesByDoctype(docType);
            if (defs != null) {
                defs.remove(workflowDefinitionId);
            }
            if (defs == null || defs.isEmpty()) {
                // Remove the entry
                docTypeDefinitionsRulesMap.remove(docType);
            } else {
                docTypeDefinitionsRulesMap.put(docType, defs);
            }
        }
    }

    public void delRuleByPath(String workflowDefinitionId, String path) {
        if (getAllowedWorkflowDefinitionNamesByPath(path).contains(
                workflowDefinitionId)) {
            Set<String> defs = getAllowedWorkflowDefinitionNamesByPath(path);
            if (defs != null) {
                defs.remove(workflowDefinitionId);
            }
            if (defs == null || defs.isEmpty()) {
                // Remove the entry
                pathDefinitionsRulesMap.remove(path);
            } else {
                pathDefinitionsRulesMap.put(path, defs);
            }
        }
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("docTypeRule")) {
                for (Object contribution : contributions) {
                    WorkflowDoctypeRuleDescriptor desc = (WorkflowDoctypeRuleDescriptor) contribution;
                    for (String id : desc.getWorkflowDefinitionIds()) {
                        addRuleByType(id, desc.getDocType());
                    }
                    log.info("Trying to register a workflow rule for type="
                            + desc.getDocType());
                }
            }
            if (extension.getExtensionPoint().equals("pathRule")) {
                for (Object contribution : contributions) {
                    WorkflowPathRuleDescriptor desc = (WorkflowPathRuleDescriptor) contribution;
                    for (String id : desc.getWorklflowDefinitionIds()) {
                        addRuleByPath(id, desc.getPath());
                    }
                    log.info("Trying to register a workflow rule for path="
                            + desc.getPath());
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("docTypeRule")) {
                for (Object contribution : contributions) {
                    WorkflowDoctypeRuleDescriptor desc = (WorkflowDoctypeRuleDescriptor) contribution;
                    for (String id : desc.getWorkflowDefinitionIds()) {
                        delRuleByType(id, desc.getDocType());
                    }
                    log.info("Trying to unregister a workflow rule for path="
                            + desc.getDocType());
                }
            }
            if (extension.getExtensionPoint().equals("pathRule")) {
                for (Object contribution : contributions) {
                    WorkflowPathRuleDescriptor desc = (WorkflowPathRuleDescriptor) contribution;
                    for (String id : desc.getWorklflowDefinitionIds()) {
                        delRuleByPath(id, desc.getPath());
                    }
                    log.info("Trying to unregister a workflow rule for path="
                            + desc.getPath());
                }
            }
        }
    }

}
