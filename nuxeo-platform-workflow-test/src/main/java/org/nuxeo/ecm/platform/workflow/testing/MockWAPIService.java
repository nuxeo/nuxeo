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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: MockWAPIService.java 29517 2008-01-22 12:41:23Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.testing;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.document.api.lifecycle.WorkflowDocumentLifeCycleManager;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;
import org.nuxeo.ecm.platform.workflow.document.api.rules.WorkflowRulesManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.WorkflowDocumentSecurityManager;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicy;
import org.nuxeo.ecm.platform.workflow.document.api.security.policy.WorkflowDocumentSecurityPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyManager;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListsManager;
import org.nuxeo.ecm.platform.workflow.document.ejb.WorkItemsListsBean;
import org.nuxeo.ecm.platform.workflow.document.ejb.WorkflowDocumentLifeCycleBean;
import org.nuxeo.ecm.platform.workflow.document.ejb.WorkflowDocumentRelationBean;
import org.nuxeo.ecm.platform.workflow.document.ejb.WorkflowDocumentSecurityBean;
import org.nuxeo.ecm.platform.workflow.document.ejb.WorkflowDocumentSecurityPolicyBean;
import org.nuxeo.ecm.platform.workflow.document.ejb.WorkflowDocumentVersioningPolicyBean;
import org.nuxeo.ecm.platform.workflow.document.service.WorkflowDocumentSecurityPolicyService;
import org.nuxeo.ecm.platform.workflow.document.service.WorkflowRulesService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Fake WAPI service
 * <p>
 * Provides adapters for beans that do not actually have a service at the
 * runtime level (just facade beans).
 * <p>
 * Also manages an entity manager for document relations.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MockWAPIService extends DefaultComponent {

    public static final String COMPONENT_NAME = "org.nuxeo.ecm.platform.workflow.testing.MockWAPIService";

    private EntityManager em;

    private WAPI wapi;

    @PersistenceContext
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public void setWAPI(WAPI wapi) {
        this.wapi = wapi;
    }

    private static class MockDocumentMessageProducer implements DocumentMessageProducer {

        public void produce(DocumentMessage message) {
        }

        public void produce(EventMessage message) {
        }

        public void produce(NXCoreEvent event) {
        }

        public void produceCoreEvents(List<NXCoreEvent> events) {
        }

        public void produceEventMessages(List<EventMessage> messages) {
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(WAPI.class)) {
            return (T) wapi;
        } else if (adapter.isAssignableFrom(WorkflowDocumentLifeCycleManager.class)) {
            return (T) new WorkflowDocumentLifeCycleBean();
        } else if (adapter.isAssignableFrom(WorkflowDocumentVersioningPolicyManager.class)) {
            return (T) new WorkflowDocumentVersioningPolicyBean();
        } else if (adapter.isAssignableFrom(WorkflowDocumentRelationManager.class)) {
            WorkflowDocumentRelationBean man = new WorkflowDocumentRelationBean();
            man.setEntityManager(em);
            return (T) man;
            // return (T) new WorkflowDocumentRelationBean();
        } else if (adapter.isAssignableFrom(WorkflowDocumentSecurityManager.class)) {
            return (T) new WorkflowDocumentSecurityBean();
        } else if (adapter.isAssignableFrom(WorkflowDocumentSecurityPolicyManager.class)) {
            return (T) new WorkflowDocumentSecurityPolicyBean();
        } else if (adapter.isAssignableFrom(WorkflowDocumentSecurityPolicy.class)) {
            // here there's a real service behind it, take the component
            return (T) Framework.getRuntime().getComponent(
                    WorkflowDocumentSecurityPolicyService.NAME);
        } else if (adapter.isAssignableFrom(WorkflowRulesManager.class)) {
            // here there's a real service behind it, take the component
            return (T) Framework.getRuntime().getComponent(
                    WorkflowRulesService.NAME);
        } else if (adapter.isAssignableFrom(WorkItemsListsManager.class)) {
            WorkItemsListsBean man = new WorkItemsListsBean();
            man.setEntityManager(em);
            return (T) man;
        } else if (adapter.isAssignableFrom(DocumentMessageProducer.class)) {
            // document message producer
            DocumentMessageProducer producer = new MockDocumentMessageProducer();
            return (T) producer;
        }
        return null;
    }
}
