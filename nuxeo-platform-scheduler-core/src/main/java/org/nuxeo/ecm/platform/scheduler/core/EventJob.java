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
 * $Id: $
 */
package org.nuxeo.ecm.platform.scheduler.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.platform.api.login.UserSession;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class EventJob implements Job {

    private static final Log log = LogFactory.getLog(EventJob.class);

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String eventId = dataMap.getString("eventId");
        String eventCategory = dataMap.getString("eventCategory");
        String username = dataMap.getString("username");

        // Setup a user session
        UserSession us = new UserSession(username,
                dataMap.getString("password"));
        try {
            us.login();
        } catch (LoginException e) {
            log.error(e);
            return;
        }
        UserPrincipal principal = new UserPrincipal(username);

        Object source = null;
        Map<String, Serializable> info = Collections.emptyMap();
        String comment = null;
        CoreEvent event = new CoreEventImpl(eventId, source, info, principal,
                eventCategory, comment);
        CoreEventListenerService service = NXCore.getCoreEventListenerService();
        if (service != null) {
            log.info("Sending scheduled event id=" + eventId + ", category="
                    + eventCategory);

            // switch to the Nuxeo classloader so that the event listeners work
            // as usual
            ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
            ClassLoader nuxeoCL = EventJob.class.getClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(nuxeoCL);
                service.notifyEventListeners(event);
            } finally {
                Thread.currentThread().setContextClassLoader(jbossCL);
            }
        } else {
            log.error("Cannot find EventListenerService");
        }
    }

}
