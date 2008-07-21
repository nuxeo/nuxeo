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
 * $Id: WorkflowDocumentRelationManager.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.relation;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;

/**
 * Workflow document relations.
 * <p>
 * Deals with many-to-many bidirectional relationship in between documents and
 * worklow instances.
 * <p>
 * A given document can take part of one or several workflow instances and a
 * workflow instances may be bound to one or several documents.
 *
 * @See org.nuxeo.ecm.platform.workflow.document.DocumentRefEntry
 *
 * @See org.nuxeo.ecm.platform.workflow.document.WorkflowInstanceRefEntry
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowDocumentRelationManager extends Serializable {

    /**
     * Return the list of workflow instance ids for document given its doc ref.
     *
     * @param docRef the core document reference
     * @return an array of String representing NXWorkflow workflow instance ids.
     * @throws WorkflowDocumentRelationException
     */
    String[] getWorkflowInstanceIdsFor(DocumentRef docRef);

    /**
     * Returns the list of document refs bound to worklfow instance given its
     * identifier.
     *
     * @param pid the workflow instance id (NXWorkflow model)
     * @return an array of document refs representing the document UUID (NXCore
     *         model)
     * @throws WorkflowDocumentRelationException
     */
    DocumentRef[] getDocumentRefsFor(String pid);

    /**
     * Creates a mapping document <-> workflow instance.
     *
     * @param docRef the NXCore document ref
     * @param workflowInstanceId the NXWorkflow workflow instance id
     * @throws WorkflowDocumentRelationException
     */
    void createDocumentWorkflowRef(DocumentRef docRef,
            String workflowInstanceId) throws WorkflowDocumentRelationException;

    /**
     * Deletes a mapping document <-> workflow instance.
     *
     * @param docRef the NXCore document ref
     * @param pid the NXWorkflow workflow instance id
     * @throws WorkflowDocumentRelationException
     */
    void deleteDocumentWorkflowRef(DocumentRef docRef,
            String pid) throws WorkflowDocumentRelationException;

    /**
     *
     * @param pids Process instance Id
     * @return map pid - document ref ids associated with this pid.
     */
    Map<String, List<String>> getDocumentModelsPids(Set<String> pids);

}
