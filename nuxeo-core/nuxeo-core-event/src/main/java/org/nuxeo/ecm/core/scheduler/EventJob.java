/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.scheduler;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginAs;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * A Quartz job whose execution sends a configured event.
 */
public class EventJob implements Job {

    private static final Log log = LogFactory.getLog(EventJob.class);

    /**
     * Job execution to send the configured event.
     */
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        // switch to the Nuxeo classloader so that the event listeners
        // work as usual

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        ClassLoader nuxeoCL = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(nuxeoCL);
        try {
            execute(dataMap);
        } catch (Exception e) {
            String eventId = dataMap.getString("eventId");
            log.error("Error while processing scheduled event id: " + eventId,
                    e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    @SuppressWarnings("unchecked")
    protected void execute(JobDataMap dataMap) throws Exception {
        String eventId = dataMap.getString("eventId");
        String eventCategory = dataMap.getString("eventCategory");
        String username = dataMap.getString("username");

        SchedulerService scheduler = Framework.getLocalService(SchedulerService.class);
        if (scheduler == null || !scheduler.hasApplicationStarted()) {
            // too early
            return;
        }

        EventService eventService = Framework.getService(EventService.class);
        if (eventService == null) {
            log.error("Cannot find EventService");
            return;
        }

        LoginContext loginContext = null;
        try {
            // login
            if (username == null) {
                loginContext = Framework.login();
            } else {
                if (Framework.getLocalService(LoginAs.class) != null) {
                    loginContext = Framework.loginAsUser(username);
                } else if (!Framework.isTestModeSet()) {
                    log.error("LoginAs service not available");
                }
            }

            // set up event context
            UserPrincipal principal = new UserPrincipal(username, null, false,
                    false);
            EventContext eventContext = new EventContextImpl(null, principal);
            eventContext.setProperty("category", eventCategory);
            eventContext.setProperties(dataMap);
            Event event = new EventImpl(eventId, eventContext);

            // start transaction
            boolean tx = TransactionHelper.startTransaction();

            // send event
            log.debug("Sending scheduled event id=" + eventId + ", category="
                    + eventCategory + ", username=" + username);
            boolean ok = false;
            try {
                eventService.fireEvent(event);
                ok = true;
            } finally {
                if (tx) {
                    if (!ok) {
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        } finally {
            // logout
            if (loginContext != null) {
                loginContext.logout();
            }
        }
    }

}
