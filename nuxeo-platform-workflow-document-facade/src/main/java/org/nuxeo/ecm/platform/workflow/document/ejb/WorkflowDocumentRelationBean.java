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
 * $Id: WorkflowDocumentRelationBean.java 26054 2007-10-16 01:50:03Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.document.ejb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.SqlResultSetMapping;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.local.WorkflowDocumentRelationLocal;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.remote.WorkflowDocumentRelationRemote;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationException;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Workflow document bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(WorkflowDocumentRelationLocal.class)
@Remote(WorkflowDocumentRelationRemote.class)
@SqlResultSetMapping(name = "worflowDocRefResult")
public class WorkflowDocumentRelationBean implements
        WorkflowDocumentRelationManager, WorkflowDocumentRelationLocal, WorkflowDocumentRelationRemote {

    private static final long serialVersionUID = -5599951695752823920L;
    private static final Log log = LogFactory.getLog(WorkflowDocumentRelationBean.class);

    @PersistenceContext(unitName = "NXWorkflowDocument")
    protected transient EntityManager em;

    public DocumentRef[] getDocumentRefsFor(String pid) {

        DocumentRef[] coreDocumentRefs;

        WorkflowInstanceRefEntry workflowInstanceRef = getWorkflowInstanceRef(pid);

        if (workflowInstanceRef != null) {
            Set<DocumentRefEntry> documentRefs = workflowInstanceRef.getDocumentRefs();
            coreDocumentRefs = new DocumentRef[documentRefs.size()];

            int i = 0;
            for (DocumentRefEntry docRef : documentRefs) {
                coreDocumentRefs[i] = docRef.getDocRef();
                i++;
            }
        } else {
            coreDocumentRefs = new DocumentRef[0];
        }

        return coreDocumentRefs;
    }

    public Map<String, List<String>> getDocumentModelsPids(
            Set<String> pids) {
        if (pids == null || pids.isEmpty()) {
            return Collections.emptyMap();
        }
        StringBuilder query = new StringBuilder("select wi from WorkflowInstanceRefEntry as wi ");
        query.append(" left join fetch wi.documentRefs ");
        query.append("where wi.workflowInstanceId in (");
        for (String pid : pids) {
            query.append("'").append(pid).append("',");
        }
        query.deleteCharAt(query.length() - 1);
        query.append(")");
        List<WorkflowInstanceRefEntry> list = em.createQuery(query.toString()).getResultList();
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (WorkflowInstanceRefEntry entry : list) {
            List<String> docIds = new ArrayList<String>();
            result.put(entry.getWorkflowInstanceId(), docIds);
            for (DocumentRefEntry idRef : entry.getDocumentRefs()) {
                docIds.add(((IdRef) idRef.getDocRef()).value);
            }
        }
        return result;
    }

    public String[] getWorkflowInstanceIdsFor(DocumentRef docRef) {

        String[] workflowInstanceIds;
        DocumentRefEntry documentRef = getDocumentRef(docRef.hashCode());

        if (documentRef != null) {
            Set<WorkflowInstanceRefEntry> workflowInstanceRefs = documentRef.getWorkflowInstanceRefs();
            workflowInstanceIds = new String[workflowInstanceRefs.size()];

            int i = 0;
            for (WorkflowInstanceRefEntry workflowInstanceRef : workflowInstanceRefs) {
                workflowInstanceIds[i] = workflowInstanceRef.getWorkflowInstanceId();
                i++;
            }
        } else {
            workflowInstanceIds = new String[0];
        }

        return workflowInstanceIds;
    }

    public void createDocumentWorkflowRef(DocumentRef coreDocRef,
            String workflowInstanceId) throws WorkflowDocumentRelationException {

        DocumentRefEntry docRef = getDocumentRef(coreDocRef.hashCode());

        if (docRef == null) {
            docRef = new DocumentRefEntry(coreDocRef);
            em.persist(docRef);
        }

        WorkflowInstanceRefEntry workflowInstanceRef;
        try {
            workflowInstanceRef = getWorkflowInstanceRef(workflowInstanceId);
        } catch (Exception e) {
            workflowInstanceRef = null;
        }
        if (workflowInstanceRef == null) {
            workflowInstanceRef = new WorkflowInstanceRefEntry(
                    workflowInstanceId);
            em.persist(workflowInstanceRef);
        }

        docRef.getWorkflowInstanceRefs().add(workflowInstanceRef);
        workflowInstanceRef.getDocumentRefs().add(docRef);
        em.flush();
    }

    public void deleteDocumentWorkflowRef(DocumentRef coreDocRef, String pid)
            throws WorkflowDocumentRelationException {

        DocumentRefEntry docRef = getDocumentRef(coreDocRef.hashCode());

        WorkflowInstanceRefEntry workflowInstanceRef = getWorkflowInstanceRef(pid);

        if (docRef != null) {
            docRef.getWorkflowInstanceRefs().remove(workflowInstanceRef);
            // Persist back if the document is still bound to workflow instances
            if (!docRef.getWorkflowInstanceRefs().isEmpty()) {
                em.persist(docRef);
            } else {
                em.remove(docRef);
            }
        }

        if (workflowInstanceRef != null) {
            workflowInstanceRef.getDocumentRefs().remove(docRef);
            // Persist back if the workflow instance is still bound to documents
            if (!workflowInstanceRef.getDocumentRefs().isEmpty()) {
                em.persist(workflowInstanceRef);
            } else {
                em.remove(workflowInstanceRef);
            }
        }
        em.flush();
    }

    private DocumentRefEntry getDocumentRef(int id) {
        DocumentRefEntry docRef = null;
        try {
            docRef = em.find(DocumentRefEntry.class, id);
        } catch (Exception e) {
            // :XXX: Hibernate bug
            // http://opensource.atlassian.com/projects/hibernate/browse/EJB-98
            // We will return null as it should
            // TODO: more robust exception handling?
            log.error(e);
        }
        return docRef;
    }

    private WorkflowInstanceRefEntry getWorkflowInstanceRef(
            String workflowInstanceId) {
        WorkflowInstanceRefEntry workflowInstanceRef = null;
        try {
            workflowInstanceRef = em.find(WorkflowInstanceRefEntry.class,
                    workflowInstanceId);
        } catch (Exception e) {
            // :XXX: Hibernate bug
            // http://opensource.atlassian.com/projects/hibernate/browse/EJB-98
            // We will return null as it should
            // TODO: more robust exception handling?
            log.error(e);
        }
        return workflowInstanceRef;
    }

    public EntityManager getEntityManager() {
        return em;
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
