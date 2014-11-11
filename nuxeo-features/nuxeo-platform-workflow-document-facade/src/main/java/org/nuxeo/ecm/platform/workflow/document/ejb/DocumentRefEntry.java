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
 * $Id: DocumentRefEntry.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Entity holding a document reference.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Entity
@Table(name = "DOCUMENT_REF")
public class DocumentRefEntry implements Serializable {

    private static final long serialVersionUID = -6695457141242248247L;

    private int id;

    private DocumentRef docRef;

    /** Corresponding workflow instance refs. */
    private Set<WorkflowInstanceRefEntry> workflowInstanceRefs = new HashSet<WorkflowInstanceRefEntry>();

    public DocumentRefEntry() {
    }

    public DocumentRefEntry(DocumentRef docRef) {
        id = docRef.hashCode();
        this.docRef = docRef;
    }

    @Id
    @Column(name = "DOC_UUID", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToMany
    @JoinTable(name = "DOCUMENT_WORKFLOW", joinColumns = { @JoinColumn(name = "DOC_UUID") },
            inverseJoinColumns = { @JoinColumn(name = "WORKFLOW_INSTANCE_ID") })
    public Set<WorkflowInstanceRefEntry> getWorkflowInstanceRefs() {
        return workflowInstanceRefs;
    }

    public void setWorkflowInstanceRefs(
            Set<WorkflowInstanceRefEntry> workflowInstanceRefs) {
        this.workflowInstanceRefs = workflowInstanceRefs;
    }

    @Column(name = "DOC_REF")
    public DocumentRef getDocRef() {
        return docRef;
    }

    public void setDocRef(DocumentRef docRef) {
        this.docRef = docRef;
    }

}
