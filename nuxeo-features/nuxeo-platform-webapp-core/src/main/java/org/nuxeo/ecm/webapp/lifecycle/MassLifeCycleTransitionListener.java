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

package org.nuxeo.ecm.webapp.lifecycle;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.events.api.JMSConstant;
import org.nuxeo.ecm.platform.events.api.impl.MassLifeCycleTransitionMessage;
import org.nuxeo.runtime.api.Framework;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NXPMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = JMSConstant.NUXEO_MESSAGE_TYPE + " = '" + JMSConstant.EVENT_MESSAGE +
                "' AND " + JMSConstant.NUXEO_EVENT_ID + " = 'massLifeCycleTransition'") })
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MassLifeCycleTransitionListener implements MessageListener {

    protected transient CoreSession documentManager;

    private static final Log log = LogFactory
            .getLog(MassLifeCycleTransitionListener.class);

    public void onMessage(Message message) {

        try {
            MassLifeCycleTransitionMessage msg = (MassLifeCycleTransitionMessage) ((ObjectMessage) message).getObject();

            String transition = msg.getTransition();
            DocumentRef ref = msg.getParentRef();
            String uri = msg.getRepository();
            String user = msg.getUser();

            LoginContext ctx = Framework.login();
            Framework.loginAs(user);

            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            documentManager = mgr.getRepository(uri).open();

            DocumentModelList docModelList = documentManager.getChildren(ref);

            // call the method to change documents state recursively
            changeDocumentsState(docModelList, transition);

            documentManager.save();
            ctx.logout();
        } catch (LoginException e) {
            log.warn("Error on login phase ", e);
        } catch (ClientException e) {
            log.warn("Client exception ", e);
        } catch (Exception e) {
            log.warn("Global exception ", e);
        }
    }

    public void changeDocumentsState(DocumentModelList docModelList,
            String transition) throws ClientException {
        for (DocumentModel docMod : docModelList) {
            if (docMod.getAllowedStateTransitions().contains(transition)) {
                docMod.followTransition(transition);

            } else {
                log.warn("Impossible to change state of " + docMod.getRef());
            }

            if (docMod.isFolder()) {
                changeDocumentsState(documentManager.getChildren(docMod
                        .getRef()), transition);
            }
        }
    }

}
