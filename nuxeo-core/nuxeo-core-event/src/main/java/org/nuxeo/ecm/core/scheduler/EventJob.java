/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.scheduler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

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
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        // switch to the Nuxeo classloader so that the event listeners
        // work as usual

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        ClassLoader nuxeoCL = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(nuxeoCL);
        try {
            execute(dataMap);
        } catch (LoginException e) {
            String eventId = dataMap.getString("eventId");
            log.error("Error while processing scheduled event id: " + eventId, e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    protected void execute(JobDataMap dataMap) throws LoginException {
        String eventId = dataMap.getString("eventId");
        String eventCategory = dataMap.getString("eventCategory");
        String username = dataMap.getString("username");

        SchedulerService scheduler = Framework.getService(SchedulerService.class);
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
                if (Framework.getService(LoginAs.class) != null) {
                    loginContext = Framework.loginAsUser(username);
                } else if (!Framework.isTestModeSet()) {
                    log.error("LoginAs service not available");
                }
            }

            // set up event context
            UserPrincipal principal = new UserPrincipal(username, null, false, false);
            EventContext eventContext = new EventContextImpl(null, principal);
            eventContext.setProperty("category", eventCategory);
            eventContext.setProperties(getWrappedMap(dataMap));
            Event event = new EventImpl(eventId, eventContext);

            // start transaction
            boolean tx = TransactionHelper.startTransaction();

            // send event
            log.debug("Sending scheduled event id=" + eventId + ", category=" + eventCategory + ", username="
                    + username);
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

    /**
     * @return a plain map from a JobDataMap object
     * @since 7.10
     */
    private Map<String, Serializable> getWrappedMap(JobDataMap jobMap) {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        for (String key : jobMap.getKeys()) {
            map.put(key, (Serializable) jobMap.get(key));
        }
        return map;
    }

}
