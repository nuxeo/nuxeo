/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.events;

import java.rmi.dgc.VMID;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.SessionContext;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.security.auth.login.LoginContext;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.event.impl.ReconnectedEventBundleImpl;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.ecm.core.event.jms.JMSEventBundle;
import org.nuxeo.ecm.core.event.jms.ReconnectedJMSEventBundle;
import org.nuxeo.runtime.api.Framework;



@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/NuxeoMessages"),
        @ActivationConfigProperty(propertyName = "providerAdapterJNDI", propertyValue = "java:/NXCoreEventsProvider"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")})
@TransactionManagement(TransactionManagementType.BEAN)
public class EventBundlesJMSListener implements MessageListener{

    private static final Log log = LogFactory.getLog(EventBundlesJMSListener.class);

    @Resource
    private SessionContext sessionContext;

    private LoginContext loginCtx;

    protected CoreSession coreSession;

    protected static boolean useTransaction = false;

    protected static boolean reconnectBundle = false;

    public void onMessage(Message jmsMessage) {

        JMSEventBundle jmsEventBundle = null;
        try {
            Object obj = ((ObjectMessage) jmsMessage).getObject();
            if (!(obj instanceof JMSEventBundle)) {
                log.debug("Message is not a JMSEventBundle, dropping" );
                return;
            }
            jmsEventBundle = (JMSEventBundle) obj;
            log.debug("Recieved an EventBundle via JMS with bundleName = " + jmsEventBundle.getEventBundleName() );

        }
        catch (JMSException e) {
            log.error("Error getting message from topic", e);
            return;
        }

        log.debug("Start processing bundle "+  jmsEventBundle.getEventBundleName());
        processJMSEventBundle(jmsEventBundle);
        log.debug("Finished processing bundle" + jmsEventBundle.getEventBundleName());
    }


    protected void processJMSEventBundle(JMSEventBundle jmsEventBundle) {

        // Check if eventBundle can be process via this JMSListener
        if (!canProcessEventBundle(jmsEventBundle)) {
            return;
        }
        try {
            login();
        }
        catch (Exception e) {
            log.error("Unable to open unrestricted session", e);
            return;
        }

        CoreSession session = null;
        try {
            EventBundle  eventBundle;
            if (reconnectBundle) {
                session = getCoreSession(jmsEventBundle.getCoreInstanceName());
                eventBundle = jmsEventBundle.reconstructEventBundle(session);
            }
            else {
                eventBundle = new ReconnectedJMSEventBundle(jmsEventBundle);
            }
            processEventBundle(eventBundle);
        } catch (Exception e) {
            log.error("Unable to get CoreSession for repository" + jmsEventBundle.getCoreInstanceName(), e);
            return;
        }
        finally {
            if (session!=null) {
                CoreInstance.getInstance().close(session);
            }
            try {
                logout();
            }
            catch (Exception le) {
                log.error("Error during logout", le);
            }
        }
    }

    protected void processEventBundle(EventBundle eventBundle) {

        EventService eventService = Framework.getLocalService(EventService.class);

        if (eventService==null) {
            log.error("Cannot reach local EventService, please check deployment");
            return;
        }

        UserTransaction transaction = null;

        if (useTransaction) {
            transaction = sessionContext.getUserTransaction();
            try {
                transaction.begin();
            } catch (Exception e) {
                log.error("Unable to start transaction, existing", e);
                return;
            }
        }

        try {
            eventService.fireEventBundle(eventBundle);
        } catch (ClientException e) {
            log.error("Error during processing of eventBundle", e);
            try {
                if (useTransaction && transaction!=null) {
                    transaction.rollback();
                }
            } catch (Exception te) {
                log.error("Error during transaction rollback", te);
            }
            return;
        } finally {
             try {
                 if (useTransaction && transaction!=null) {
                     transaction.commit();
                 }
             }
             catch (Exception te) {
                 log.error("Error during transaction commit", te);
             }
        }
    }

    protected boolean canProcessEventBundle(JMSEventBundle jmsEventBundle) {

        // by default only events coming from another VM can be processed via JMS forwarding
        // forceJMS flag can be used to override this
        VMID sourceVMID = jmsEventBundle.getSourceVMID();
        if (AsyncProcessorConfig.forceJMSUsage()) {
            return true;
        } else {
            return !getCurrentVMId().equals(sourceVMID);
        }
    }

    protected VMID getCurrentVMId() {
        return EventServiceImpl.VMID;
    }

    protected CoreSession getCoreSession(String repoName) throws Exception {

        RepositoryManager mgr;

        mgr = Framework.getService(RepositoryManager.class);
        Repository repo;
        if (repoName != null) {
            repo = mgr.getRepository(repoName);
        } else {
            repo = mgr.getDefaultRepository();
        }

        if (repo == null) {
            log.error("can not find repository");
            return null;
        }

        return repo.open();
    }


    private void login() throws Exception {
        loginCtx = Framework.login();
    }

    private void logout() throws Exception {
        if (loginCtx != null) {
            loginCtx.logout();
            loginCtx = null;
        }
    }

}
