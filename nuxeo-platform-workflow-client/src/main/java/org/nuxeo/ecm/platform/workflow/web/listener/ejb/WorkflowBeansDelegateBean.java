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
 * $Id: WorkflowBeansDelegateBean.java 28960 2008-01-11 13:37:02Z tdelprat $
 */

package org.nuxeo.ecm.platform.workflow.web.listener.ejb;

import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.ui.web.shield.NuxeoJavaBeanErrorHandler;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkItemsListsBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentLifeCycleBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentSecurityPolicyBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentVersioningPolicyBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowRulesBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.lifecycle.WorkflowDocumentLifeCycleManager;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListsManager;
import org.nuxeo.ecm.platform.workflow.web.api.WorkflowBeansDelegate;

/**
 * Workflow beans delegate bean.
 *
 * <p>
 * Leverages the delegates and acts as a central access point to core workflow
 * session beans.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@Name("workflowBeansDelegate")
@NuxeoJavaBeanErrorHandler
public class WorkflowBeansDelegateBean implements WorkflowBeansDelegate {

    private static final long serialVersionUID = 8589557822791361833L;

    protected static final Log log = LogFactory.getLog(WorkflowBeansDelegateBean.class);

    @In(required = true)
    protected RepositoryLocation currentServerLocation;

    protected WAPI wapi;

    protected DocumentMessageProducer docMsgProducer;

    protected WorkflowDocumentSecurityBusinessDelegate wFSecurityManagerBD;

    protected WorkflowDocumentLifeCycleBusinessDelegate wFDocLifeCycleManagerBD;

    protected WorkflowDocumentRelationBusinessDelegate wDocBusinessDelegate;

    protected WorkflowRulesBusinessDelegate wFRulesBD;

    protected WorkflowDocumentVersioningPolicyBusinessDelegate wFVersioningBD;

    protected WorkflowDocumentSecurityPolicyBusinessDelegate wFDocSecuPolicyBD;

    protected WorkItemsListsBusinessDelegate wiListsBD;

    @Create
    public void init()
    {
        initializeBD();
        Events.instance().raiseEvent(EventNames.WF_INIT);
    }

    public WorkflowBeansDelegateBean() {
        initializeBD();
    }

    private void initializeBD() {
        try {
            wapi = WAPIBusinessDelegate.getWAPI();
        } catch (WMWorkflowException e) {
            log.error("Cannot get WAPIBean...");
        }
        wDocBusinessDelegate = new WorkflowDocumentRelationBusinessDelegate();
        wFRulesBD = new WorkflowRulesBusinessDelegate();
        wFVersioningBD = new WorkflowDocumentVersioningPolicyBusinessDelegate();
        wFDocSecuPolicyBD = new WorkflowDocumentSecurityPolicyBusinessDelegate();
        wiListsBD = new WorkItemsListsBusinessDelegate();
        wFSecurityManagerBD = new WorkflowDocumentSecurityBusinessDelegate();
        wFDocLifeCycleManagerBD = new WorkflowDocumentLifeCycleBusinessDelegate();
    }

    public WAPI getWAPIBean() throws WMWorkflowException {
        if (wapi == null) {
            try {
                wapi = WAPIBusinessDelegate.getWAPI();
            } catch (Exception e) {
                throw new WMWorkflowException(e);
            } finally {
                if (wapi == null) {
                    throw new WMWorkflowException("WAPI bean is null..."
                            + " sorry cancelling..");
                }
            }
        }
        return wapi;
    }

    public WorkflowDocumentSecurityManager getWFSecurityManagerBean()
            throws WMWorkflowException {
        WorkflowDocumentSecurityManager workflowSecurityManager;
        try {
            String repositoryUri = currentServerLocation.getName();
            workflowSecurityManager = wFSecurityManagerBD.getWorkflowSecurityManager(repositoryUri);
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }
        if (workflowSecurityManager == null) {
            throw new WMWorkflowException(
                    "WorkflowDocumentSecurityManager is null..."
                            + " Sorry cancelling..");
        }
        return workflowSecurityManager;
    }

    public WorkflowDocumentRelationManager getWorkflowDocumentBean()
            throws WMWorkflowException {
        WorkflowDocumentRelationManager wDoc;
        try {
            wDoc = wDocBusinessDelegate.getWorkflowDocument();
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }
        if (wDoc == null) {
            throw new WMWorkflowException("WorkflowDocumentBean is null..."
                    + " Sorry cancelling..");
        }
        return wDoc;
    }

    public WorkflowDocumentLifeCycleManager getWfDocLifeCycleManagerBean()
            throws WMWorkflowException {
        WorkflowDocumentLifeCycleManager manager;
        try {
            String repositoryUri = currentServerLocation.getName();
            manager = wFDocLifeCycleManagerBD.getWorkflowDocumentLifeCycleManager(repositoryUri);
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }
        if (manager == null) {
            throw new WMWorkflowException(
                    "WorkflowDocumentLifeCycleManager is null..."
                            + " Sorry cancelling..");
        }
        return manager;
    }

    public WorkflowRulesManager getWorkflowRulesBean()
            throws WMWorkflowException {
        WorkflowRulesManager rules;
        try {
            rules = wFRulesBD.getWorkflowRules();
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }
        if (rules == null) {
            throw new WMWorkflowException("WorkflowRulesBean is null..."
                    + " Sorry cancelling..");
        }
        return rules;
    }

    public DocumentMessageProducer getDocumentMessageProducer()
            throws WMWorkflowException {
        if (docMsgProducer == null) {
            try {
                docMsgProducer = DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
            } catch (Exception e) {
                throw new WMWorkflowException(e);
            } finally {
                if (docMsgProducer == null) {
                    throw new WMWorkflowException(
                            "DocumentMessageProducerBean is null..."
                                    + " Sorry cancelling..");
                }
            }
        }
        return docMsgProducer;
    }

    public WorkflowDocumentVersioningPolicyManager getWorkflowVersioningPolicy()
            throws WMWorkflowException {
        WorkflowDocumentVersioningPolicyManager versioningPolicy;
        try {
            versioningPolicy = wFVersioningBD.getWorkflowVersioningPolicy();
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }
        if (versioningPolicy == null) {
            throw new WMWorkflowException(
                    "WorkflowVersioningPolicyBean is null...."
                            + " sorry cancelling");
        }
        return versioningPolicy;
    }

    public WorkflowDocumentSecurityPolicyManager getWorkflowDocumentSecurityPolicy()
            throws WMWorkflowException {
        WorkflowDocumentSecurityPolicyManager securityPolicy;
        try {
            securityPolicy = wFDocSecuPolicyBD.getWorkflowDocumentRightsPolicyManager();
        } catch (Exception e) {
            throw new WMWorkflowException(e);
        }
        if (securityPolicy == null) {
            throw new WMWorkflowException(
                    "WorkflowDocumentRightsPolicyBean is null..."
                            + " sorry cancelling...");
        }
        return securityPolicy;
    }

    public WorkItemsListsManager getWorkItemsLists()
            throws WorkItemsListException {
        WorkItemsListsManager wiLists;
        try {
            wiLists = wiListsBD.getWorkItemLists();
        } catch (Exception e) {
            throw new WorkItemsListException(e);
        }
        if (wiLists == null) {
            throw new WorkItemsListException("WorkItemListsBean is null..."
                    + " sorry cancelling...");
        }
        return wiLists;
    }

    @Remove
    public void destroy() {
        log.debug("Removing WorkflowBeansDelegate SEAM component...");
        wapi = null;
        wDocBusinessDelegate = null;
        wFDocLifeCycleManagerBD = null;
        wFSecurityManagerBD = null;
        wFRulesBD = null;
        wFVersioningBD = null;
        wFDocSecuPolicyBD = null;
        wiListsBD = null;
    }

}
