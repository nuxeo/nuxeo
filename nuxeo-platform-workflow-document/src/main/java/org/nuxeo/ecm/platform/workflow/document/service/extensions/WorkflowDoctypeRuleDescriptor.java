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
 * $Id: WorkflowDoctypeRuleDescriptor.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.service.extensions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Workflow doctype rule descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("docTypeRule")
public class WorkflowDoctypeRuleDescriptor implements Serializable {

    private static final long serialVersionUID = 2148816431563460697L;

    @XNodeList(value = "workflowDefinition", type = ArrayList.class, componentType = String.class)
    List<String> workflowDefinitionIds = new ArrayList<String>();

    @XNode("@docType")
    private String docType;

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public List<String> getWorkflowDefinitionIds() {
        return workflowDefinitionIds;
    }

    public void setWorkflowDefinitionIds(List<String> workflowDefinitionIds) {
        this.workflowDefinitionIds = workflowDefinitionIds;
    }

}
