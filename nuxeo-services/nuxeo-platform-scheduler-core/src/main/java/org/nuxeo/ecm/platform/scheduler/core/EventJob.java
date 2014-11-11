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

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.platform.api.login.UserSession;
import org.nuxeo.runtime.api.Framework;
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
        EventContext ctx = new EventContextImpl(null, principal);
        ctx.setProperty("category", eventCategory);
        Event event = new EventImpl(eventId, ctx);
        EventService evtService = null;

        try {
            evtService = Framework.getService(EventService.class);
        }
        catch (Exception e) {
            log.error("Cannot find EventService", e);
        }

        if (evtService != null) {
            log.info("Sending scheduled event id=" + eventId + ", category="
                    + eventCategory);

            // switch to the Nuxeo classloader so that the event listeners work
            // as usual
            ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
            ClassLoader nuxeoCL = EventJob.class.getClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(nuxeoCL);
                evtService.fireEvent(event);
            } catch (ClientException e) {
                log.error("Error while sending event to EventService", e);
            }

            finally {
                Thread.currentThread().setContextClassLoader(jbossCL);
            }
        } else {
            log.error("Cannot find EventService");
        }
    }

}
