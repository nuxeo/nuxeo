/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: AbstractWorkflowDocumentHandler.java 22663 2007-07-17 14:13:36Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventTypes;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.CoreDocumentManagerBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentLifeCycleBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityPolicyBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.lifecycle.WorkflowDocumentLifeCycleManager;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler;
import org.nuxeo.ecm.platform.workflow.jbpm.util.IDConverter;

/**
 * Base handler for both jBPM action and assignment handlers.
 * <p>
 * Defines an API to interact with NXWorkflow and NXWorkflowDocument from jBPM
 * handlers.
 *
 * Expected to be used and extended to define custom business rules around
 * process dealing with documents.
 * <p>
 * Current implementation assumes only one document bound to the process...
 *
 * @see org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler
 * @see org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentAssignmentHandler
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractWorkflowDocumentHandler {

    protected static final Log log = LogFactory.getLog(AbstractWorkflowDocumentActionHandler.class);

    protected final WorkflowDocumentLifeCycleBusinessDelegate lifecycleBD = new WorkflowDocumentLifeCycleBusinessDelegate();

    protected final WorkflowDocumentSecurityBusinessDelegate secuBD = new WorkflowDocumentSecurityBusinessDelegate();

    protected final WorkflowDocumentSecurityPolicyBusinessDelegate secuPolicyBD = new WorkflowDocumentSecurityPolicyBusinessDelegate();

    protected final WorkflowDocumentRelationBusinessDelegate wDocRelBD = new WorkflowDocumentRelationBusinessDelegate();

    protected final CoreDocumentManagerBusinessDelegate coreDocBD = new CoreDocumentManagerBusinessDelegate();

    /**
     * Returns the workflow document life cycle manager bean.
     *
     * @param ec the jbpm execution context.
     * @return an EJB proxy
     * @throws Exception
     */
    protected WorkflowDocumentLifeCycleManager getLifeCycleManager(
            ExecutionContext ec) throws Exception {
        String repositoryUri = getDocumentRepositoryLocationURI(ec);
        if (repositoryUri != null) {
            return lifecycleBD.getWorkflowDocumentLifeCycleManager(repositoryUri);
        } else {
            throw new Exception("No repository URI.... Cancelling...");
        }
    }

    /**
     * Returns the document manager bean (core session).
     *
     * @param ec the jbpm execution context.
     * @return an EJB proxy
     * @throws Exception
     */
    protected CoreSession getDocumentManager(ExecutionContext ec)
            throws Exception {
        String repositoryUri = getDocumentRepositoryLocationURI(ec);
        if (repositoryUri != null) {
            return coreDocBD.getDocumentManager(repositoryUri, null);
        } else {
            throw new Exception("No repository URI.... Cancelling...");
        }
    }

    /**
     * Returns the document message producer bean.
     *
     * @return an EJB proxy
     * @throws NamingException
     */
    protected DocumentMessageProducer getDocumentMessageProducer()
            throws Exception {
        return DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
    }

    /**
     * Returns the workflow api bean.
     *
     * @return en EJB proxy
     * @throws Exception
     */
    protected WAPI getWAPI() throws Exception {
        return WAPIBusinessDelegate.getWAPI();
    }

    /**
     * Returns the workflow document security manager bean.
     *
     * @param ec the jbpm execution context
     * @return en EJB proxy
     * @throws Exception
     */
    protected WorkflowDocumentSecurityManager getSecuManager(ExecutionContext ec)
            throws Exception {
        String repositoryUri = getDocumentRepositoryLocationURI(ec);
        if (repositoryUri != null) {
            WorkflowDocumentSecurityManager manager = secuBD.getWorkflowSecurityManager(repositoryUri);
            return manager;
        } else {
            throw new Exception("No repository URI... Cancelling....");
        }
    }

    /**
     * Returns the worflow document security policy manager bean.
     *
     * @return en EJB proxy
     * @throws Exception
     */
    protected WorkflowDocumentSecurityPolicyManager getSecuPolicyManager()
            throws Exception {
        return secuPolicyBD.getWorkflowDocumentRightsPolicyManager();
    }

    /**
     * Returns the workflow document relation manager bean.
     *
     * @return an EJB proxy
     * @throws Exception
     */
    protected WorkflowDocumentRelationManager getWorkflowDocumentRelation()
            throws Exception {
        return wDocRelBD.getWorkflowDocument();
    }

    /**
     * Notify event.
     * <p>
     * Uses the document message producer bean which forwards on the NXP JMS
     * topic.
     *
     * @param ec the jbpm execution context
     * @throws Exception
     */
    protected void notifyEvent(ExecutionContext ec, String eventId)
            throws Exception {

        DocumentMessageProducer producer = getDocumentMessageProducer();
        WAPI wapi = getWAPI();

        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());

        WMProcessInstance wi = wapi.getProcessInstanceById(pid,
                WorkflowConstants.WORKFLOW_INSTANCE_STATUS_ACTIVE);

        String category = "";
        if (wi != null) {
            category = wi.getName();
        }

        String state = getDocumentCurrentLifeCycle(ec);
        String comment = category;

        DocumentModel dm = getDocumentModel(ec);

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreEventConstants.DOC_LIFE_CYCLE, state);

        String evtId = (eventId == null) ? WorkflowEventTypes.WORKFLOW_ENDED
                : eventId;
        CoreEvent event = new CoreEventImpl(evtId, dm, props,
                wapi.getParticipant(), category, comment);

        DocumentMessage msg = new DocumentMessageImpl(dm, event);
        producer.produce(msg);
    }

    /**
     * Returns the actual process instance.
     *
     * @param ec the jbpm execution context
     * @return a jbpm process instance.
     * @throws Exception
     */
    protected ProcessInstance getProcessInstance(ExecutionContext ec) {
        return ec.getProcessInstance();
    }

    /**
     * Returns the actual process creator.
     *
     * @param ec the jbpm execution context
     * @return the principal name
     * @throws Exception
     */
    protected String getProcessInstanceCreator(ExecutionContext ec)
            throws Exception {
        return (String) getProcessInstance(ec).getContextInstance().getVariable(
                WorkflowConstants.WORKFLOW_CREATOR);
    }

    /**
     * Returns the actual process instance modification policy.
     *
     * @param ec the jbpm execution context
     * @return the modification policy
     * @throws Exception
     */
    protected String getProcessInstanceDocumentModificationPolicy(
            ExecutionContext ec) throws Exception {
        return (String) getProcessInstance(ec).getContextInstance().getVariable(
                WorkflowConstants.DOCUMENT_MODIFICATION_POLICY);
    }

    /**
     * Returns the actual process instance versioning policy.
     *
     * @param ec the jbpm execution context
     * @return the versioning policy
     * @throws Exception
     */
    protected String getProcessInstanceDocumentVersioningPolicy(
            ExecutionContext ec) throws Exception {
        return (String) getProcessInstance(ec).getContextInstance().getVariable(
                WorkflowConstants.DOCUMENT_VERSIONING_POLICY);
    }

    /**
     * Returns the actual process instance review level.
     *
     * @param ec the jbpm execution context
     * @return the current review level. Default is 0 if not found.
     * @throws Exception
     */
    protected int getProcessInstanceCurrentReviewLevel(ExecutionContext ec)
            throws Exception {
        Object value = getProcessInstance(ec).getContextInstance().getVariable(
                WorkflowConstants.WORKFLOW_REVIEW_LEVEL);
        if (value != null) {
            return (Integer) value;
        }
        return 0;
    }

    /**
     * Returns the document ref if one is bound to the actual process.
     *
     * @param ec the jbpm execution context
     * @return a Nuxeo core document reference
     */
    protected DocumentRef getDocumentRef(ExecutionContext ec) {
        return (DocumentRef) ec.getVariable(WorkflowConstants.DOCUMENT_REF);
    }

    /**
     * Returns the document model if one is bound to the actual process.
     *
     * @param ec the jbpm execution context
     * @return a Nuxeo core document model instance
     * @throws Exception
     */
    protected DocumentModel getDocumentModel(ExecutionContext ec)
            throws Exception {
        // :XXX: shouldn't be fetch from this bean.
        return getLifeCycleManager(ec).getDocumentModelFor(getDocumentRef(ec));
    }

    /**
     * Returns the repository location URI if a document ref is bound to the
     * actual process.
     * <p>
     * Repository location are mandatory beside document ref to identify a
     * document using Nuxeo core remoting, currently. (5.1.x)
     *
     * @param ec the jbpm execution context
     * @return a repository URI
     */
    protected String getDocumentRepositoryLocationURI(ExecutionContext ec) {
        return (String) ec.getVariable(WorkflowConstants.DOCUMENT_LOCATION_URI);
    }

    /**
     * Returns the transition to destination life cycle after the process if any
     * bound to the actual process.
     *
     * @param ec the jbpm execution context
     * @return a life cycle transition name.
     */
    protected String getLifeCycleTransitionToDestinationState(
            ExecutionContext ec) {
        String transition = (String) ec.getVariable(WorkflowConstants.LIFE_CYCLE_TRANSITION_TO_DESTINATION_STATE);
        return transition;
    }

    /**
     * Returns the allows state transitions for the document bound the process
     * if any.
     *
     * @param ec the jbpm execution context
     * @return a collection of life cycle state names.
     * @throws Exception
     */
    protected Collection<String> getDocumentAllowedStateTransitions(
            ExecutionContext ec) throws Exception {
        Collection<String> ats;
        WorkflowDocumentLifeCycleManager wfLifeCycleManager = getLifeCycleManager(ec);
        DocumentRef docRef = getDocumentRef(ec);
        ats = wfLifeCycleManager.getAllowedStateTransitions(docRef);
        return ats;
    }

    /**
     * Makes the document bound to the process, if it exists, follow a
     * transition.
     *
     * @param ec the jbpm execution context
     * @param transition a life cycle transtion name
     * @return true of operation succedeed false if not
     * @throws Exception
     */
    protected boolean documentFollowTransition(ExecutionContext ec,
            String transition) throws Exception {
        boolean res = false;
        DocumentRef docRef = getDocumentRef(ec);
        if (docRef == null) {
            log.error("Cannot find associated document ref on process...");
        } else {
            WorkflowDocumentLifeCycleManager wfLifeCycleManager = getLifeCycleManager(ec);
            Collection<String> ats = getDocumentAllowedStateTransitions(ec);
            if (ats.contains(transition)) {
                res = wfLifeCycleManager.followTransition(docRef, transition);
            } else {
                log.error("Unknown transition "+ transition);
            }
        }
        return res;
    }

    /**
     * Returns the current life cycle state for the document bound to the
     * process if any.
     *
     * @param ec the jbpm execution context
     * @return the current life cycle state name.
     * @throws Exception
     */
    protected String getDocumentCurrentLifeCycle(ExecutionContext ec)
            throws Exception {
        DocumentRef docRef = getDocumentRef(ec);
        WorkflowDocumentLifeCycleManager manager = getLifeCycleManager(ec);
        String state = manager.getCurrentLifeCycleState(docRef);
        return state;
    }

    /**
     * Bind the actual process with the document, if any, given an execution
     * context.
     *
     * @param ec the jbpm execution context
     * @throws Exception
     */
    protected void bindDocumentToProcess(ExecutionContext ec) throws Exception {
        WorkflowDocumentRelationManager docRelManager = getWorkflowDocumentRelation();
        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());
        DocumentRef docRef = getDocumentRef(ec);
        if (docRef != null) {
            docRelManager.createDocumentWorkflowRef(docRef, pid);
        } else {
            log.error("Cannot bind document to process..doc ref not found");
        }
    }

    /**
     * Unbinds the document from the process if exists.
     *
     * @param ec the jbpm execution context
     * @throws Exception
     */
    protected void unbindDocumentToProcess(ExecutionContext ec)
            throws Exception {
        WorkflowDocumentRelationManager docRelManager = getWorkflowDocumentRelation();
        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());
        DocumentRef docRef = getDocumentRef(ec);
        if (docRef != null) {
            docRelManager.deleteDocumentWorkflowRef(docRef, pid);
        } else {
            log.error("Cannot bind document to process..doc ref not found");
        }
    }

    /**
     * Removes the ACL the actual process added on the document if one is bound
     * to the process.
     *
     * @param ec the jbpm execution context
     * @throws Exception
     */
    protected void removeWFACL(ExecutionContext ec) throws Exception {
        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());
        WorkflowDocumentSecurityManager manager = getSecuManager(ec);
        DocumentRef docRef = getDocumentRef(ec);
        manager.removeACL(docRef, pid);
    }

    /**
     * Returns the process instance name.
     *
     * @param ec the jbpm execution context
     * @return the process instance name
     * @throws Exception
     */
    protected String getProcessInstanceName(ExecutionContext ec)
            throws Exception {
        return getProcessInstance(ec).getProcessDefinition().getName();
    }

    /**
     * Setup the rights on the document bound to the actual process, if any,
     * using the security policy bound to the process, if any.
     *
     * @param ec the jbpm execution context
     * @throws Exception
     */
    protected void setupRightsFromPolicy(ExecutionContext ec) throws Exception {
        WorkflowDocumentSecurityManager workflowSecurityManager = getSecuManager(ec);
        WorkflowDocumentSecurityPolicyManager secuPolicyManager = getSecuPolicyManager();
        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());
        WorkflowDocumentSecurityPolicy policy = secuPolicyManager.getWorkflowDocumentSecurityPolicyFor(getProcessInstanceName(ec));
        if (policy != null) {
            List<UserEntry> userEntries = policy.getRules(pid, getInfoMap(ec));
            workflowSecurityManager.setRules(getDocumentRef(ec), userEntries,
                    pid);
        }
    }

    /**
     * Setup the default rights on the document bound to the actual process, if
     * any, using the security policy bound to the process, if any.
     *
     * @param ec the jbpm execution context
     * @throws Exception
     */
    protected void setupDefaultRightsFromPolicy(ExecutionContext ec)
            throws Exception {
        WorkflowDocumentSecurityManager workflowSecurityManager = getSecuManager(ec);
        WorkflowDocumentSecurityPolicyManager secuPolicyManager = getSecuPolicyManager();
        String pid = IDConverter.getNXWorkflowIdentifier(getProcessInstance(ec).getId());
        WorkflowDocumentSecurityPolicy policy = secuPolicyManager.getWorkflowDocumentSecurityPolicyFor(getProcessInstanceName(ec));
        if (policy != null) {
            List<UserEntry> userEntries = policy.getDefaultRules(pid,
                    getInfoMap(ec));
            workflowSecurityManager.setRules(getDocumentRef(ec), userEntries,
                    pid);
        }
    }

    /**
     * Computes a process info map => optimization.
     *
     * @param ec the jbpm execution context.
     * @return a process info map.
     * @throws Exception
     */
    protected Map<String, Serializable> getInfoMap(ExecutionContext ec)
            throws Exception {
        Map<String, Serializable> infos = new HashMap<String, Serializable>();
        infos.put(WorkflowConstants.WORKFLOW_CREATOR,
                getProcessInstanceCreator(ec));
        infos.put(WorkflowConstants.DOCUMENT_MODIFICATION_POLICY,
                getProcessInstanceDocumentModificationPolicy(ec));
        infos.put(WorkflowConstants.DOCUMENT_VERSIONING_POLICY,
                getProcessInstanceDocumentVersioningPolicy(ec));
        infos.put(WorkflowConstants.WORKFLOW_REVIEW_LEVEL,
                getProcessInstanceCurrentReviewLevel(ec));
        return infos;
    }

}
