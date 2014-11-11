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
package org.nuxeo.ecm.platform.scheduler.core;

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.runtime.api.Framework;
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
        try {
            Thread.currentThread().setContextClassLoader(nuxeoCL);
            execute(dataMap);
        } catch (Exception e) {
            log.error(e, e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    @SuppressWarnings("unchecked")
    protected void execute(JobDataMap dataMap) throws Exception {
        String eventId = dataMap.getString("eventId");
        String eventCategory = dataMap.getString("eventCategory");
        String username = dataMap.getString("username");

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
                loginContext = NuxeoAuthenticationFilter.loginAs(username);
            }

            // set up event context
            UserPrincipal principal = new UserPrincipal(username, null, false,
                    false);
            EventContext eventContext = new EventContextImpl(null, principal);
            eventContext.setProperty("category", eventCategory);
            eventContext.setProperties(getWrappedMap(dataMap));
            Event event = new EventImpl(eventId, eventContext);

            // start transaction
            TransactionHelper.startTransaction();

            // send event
            log.info("Sending scheduled event id=" + eventId + ", category="
                    + eventCategory + ", username=" + username);
            eventService.fireEvent(event);
        } finally {
            // finish transaction
            TransactionHelper.commitOrRollbackTransaction();

            // logout
            if (loginContext != null) {
                loginContext.logout();
            }
        }
    }

    private Map<String, Serializable> getWrappedMap(JobDataMap jobMap) {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        for (String key : jobMap.getKeys()) {
            map.put(key, (Serializable)jobMap.get(key));
        }
        return map;
    }

}
