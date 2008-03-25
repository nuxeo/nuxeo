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
 * $Id$
 */

package org.nuxeo.ecm.platform.publishing.workflow;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.publishing.PublishingServiceImpl;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityPolicyBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Publishing message listener.
 * <p>
 * Listens for messages on the NXP topic to trigger publishing operations.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@TransactionManagement(TransactionManagementType.CONTAINER)
public class PublishingListener implements MessageListener {

    private static final Log log = LogFactory.getLog(PublishingListener.class);

    private WAPI wapi;

    private WorkflowDocumentSecurityManager secuManager;

    private WorkflowDocumentSecurityPolicyManager secuPolicyManager;

    private PublishingService publishingService;

    private PublishingService getPublishingService() {
        if (publishingService == null) {
            publishingService = (PublishingService) Framework.getRuntime().getComponent(
                    PublishingServiceImpl.NAME);
        }
        return publishingService;
    }

    private WAPI getWAPI() throws WMWorkflowException {
        if (wapi == null) {
            wapi = WAPIBusinessDelegate.getWAPI();
        }
        return wapi;
    }

    private WorkflowDocumentSecurityManager getSecuManager(String repoName)
            throws Exception {
        if (secuManager == null) {
            secuManager = new WorkflowDocumentSecurityBusinessDelegate().getWorkflowSecurityManager(repoName);
        }
        return secuManager;
    }

    private WorkflowDocumentSecurityPolicyManager getSecuPolicyManager()
            throws Exception {
        if (secuPolicyManager == null) {
            secuPolicyManager = new WorkflowDocumentSecurityPolicyBusinessDelegate().getWorkflowDocumentRightsPolicyManager();
        }
        return secuPolicyManager;
    }

    private WMProcessDefinition getPublishingProcessDefinition()
            throws WMWorkflowException {
        WAPI wapi = getWAPI();
        return wapi.getProcessDefinitionByName(PublishingConstants.WORKFLOW_DEFINITION_NAME);
    }

    private String[] getReviewers(DocumentModel dm) throws ClientException {
        try {
            return getPublishingService().getValidatorsFor(dm);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    private void moderate(DocumentMessage msg) throws Exception {

        log.info("Moderation will occur for dm=" + msg.getPathAsString());

        // Start actual publishing workflow.
        WAPI wapi = getWAPI();
        WMProcessDefinition pd = getPublishingProcessDefinition();

        // Create the workflow parameters map.
        Map<String, Serializable> vars = new HashMap<String, Serializable>();

        vars.put(WorkflowConstants.DOCUMENT_REF, msg.getRef());
        vars.put(WorkflowConstants.DOCUMENT_LOCATION_URI,
                msg.getRepositoryName());
        vars.put(PublishingConstants.WORKFLOW_REVIEWERS, getReviewers(msg));
        vars.put(PublishingConstants.SUBMITTED_BY, msg.getPrincipalName());

        // Start the process
        WMActivityInstance activity = wapi.startProcess(pd.getId(), vars, null);

        // Get the current activity after startup
        Collection<WMActivityInstance> activities = wapi.getActivityInstancesFor(activity.getProcessInstance().getId());
        if (!activities.isEmpty()) {
            // XXX Assume only one activity right now.
            activity = activities.iterator().next();
        }

        // Ask the workflow to setup rights.
        // wapi.followTransition(activity,
        // PublishingConstants.WORKFLOW_TRANSITION_TO_RIGHTS, null);

        // XXX this should be done using the above code.
        WMProcessInstance pi = activity.getProcessInstance();
        WorkflowDocumentSecurityManager workflowSecurityManager = getSecuManager(msg.getRepositoryName());
        WorkflowDocumentSecurityPolicyManager secuPolicyManager = getSecuPolicyManager();
        WorkflowDocumentSecurityPolicy policy = secuPolicyManager.getWorkflowDocumentSecurityPolicyFor(
                activity.getProcessInstance().getName());
        if (policy != null) {
            List<UserEntry> userEntries = policy.getRules(pi.getId(), null);
            workflowSecurityManager.setRules(msg.getRef(), userEntries,
                    pi.getId());
        }
    }

    public void onMessage(Message message) {

        try {

            final Serializable obj = ((ObjectMessage) message).getObject();
            if (!(obj instanceof DocumentMessage)) {
                log.debug("Not a DocumentMessage instance embedded ignoring.");
                return;
            }

            final DocumentMessage msg = (DocumentMessage) obj;
            if (msg.getEventId().equals(EventNames.PROXY_PUSLISHING_PENDING)) {
                moderate(msg);
            } else if (msg.getEventId().equals("")) {

            }

        } catch (Exception e) {
            throw new EJBException(e);
        }
    }
}
