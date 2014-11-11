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
 * $Id: WorkflowInstanceRefEntry.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * Entity holding a workflow instance reference.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Entity
@Table(name = "WORKFLOW_INSTANCE_REF")
public class WorkflowInstanceRefEntry implements Serializable {

    private static final long serialVersionUID = -6919922984287443049L;

    /** NXWorkflow instance identifier. */
    // :XXX: Add an integer as primary key for better performances.
    protected String workflowInstanceId;

    /** Corresponding document refs. */
    private Set<DocumentRefEntry> documentRefs = new HashSet<DocumentRefEntry>();

    public WorkflowInstanceRefEntry() {
    }

    public WorkflowInstanceRefEntry(String workflowInstanceID) {
        workflowInstanceId = workflowInstanceID;
    }

    @Id
    @Column(name = "WORKFLOW_INSTANCE_ID", nullable = false)
    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public void setDocumentRefs(Set<DocumentRefEntry> documentRefs) {
        this.documentRefs = documentRefs;
    }

    @ManyToMany(mappedBy = "workflowInstanceRefs")
    public Set<DocumentRefEntry> getDocumentRefs() {
        return documentRefs;
    }

}
