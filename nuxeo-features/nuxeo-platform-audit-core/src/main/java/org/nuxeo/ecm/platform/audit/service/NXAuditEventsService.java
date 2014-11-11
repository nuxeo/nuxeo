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

package org.nuxeo.ecm.platform.audit.service;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryFactory;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.extension.EventDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.LogEntryFactoryDescriptor;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Event service configuration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class NXAuditEventsService extends DefaultComponent implements
        NXAuditEvents {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.audit.service.NXAuditEventsService");

    private static final Set<String> eventNames = new HashSet<String>();

    // Default.
    private static Class<LogEntryFactory> logEntryFactoryKlass;

    private static final String EVENT_EXT_POINT = "event";

    private static final String FACTORY_EXT_POINT = "logEntryFactory";

    private static final Log log = LogFactory.getLog(NXAuditEventsService.class);

    public Set<String> getAuditableEventNames() {
        return eventNames;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(EVENT_EXT_POINT)) {
                for (Object contribution : contributions) {
                    EventDescriptor desc = (EventDescriptor) contribution;
                    String eventName = desc.getName();
                    Boolean eventEnabled = desc.getEnabled();
                    if (eventEnabled == null) {
                        eventEnabled = true; 
                    }
                    if (eventEnabled){
                        eventNames.add(eventName);   
                        log.debug("Registered event: " + eventName);                     
                    } else if (eventNames.contains(eventName) && !eventEnabled){                      
                        eventNames.remove(eventName);
                        log.debug("Unregistered event: " + eventName);
                    }
                }
            }
            if (extension.getExtensionPoint().equals(FACTORY_EXT_POINT)) {
                for (Object contribution : contributions) {
                    LogEntryFactoryDescriptor desc = (LogEntryFactoryDescriptor) contribution;
                    log.debug("Registered factory: " +
                            desc.getKlass().getName());
                    logEntryFactoryKlass = desc.getKlass();
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(EVENT_EXT_POINT)) {
                for (Object contribution : contributions) {
                    EventDescriptor desc = (EventDescriptor) contribution;
                    log.debug("Unregistered event: " + desc.getName());
                    eventNames.remove(desc.getName());
                }
            }
            if (extension.getExtensionPoint().equals(FACTORY_EXT_POINT)) {
                for (Object contribution : contributions) {
                    LogEntryFactoryDescriptor desc = (LogEntryFactoryDescriptor) contribution;
                    if (logEntryFactoryKlass == desc.getKlass()) {
                        log.debug("Unregistered factory: " +
                                desc.getKlass().getName());
                        logEntryFactoryKlass = null;
                    }
                }
            }
        }
    }

    public Class<LogEntryFactory> getLogEntryFactoryKlass() {
        return logEntryFactoryKlass;
    }

    public LogEntry computeLogEntry(DocumentMessage doc) {
        LogEntry logEntry = null;
        LogEntryFactory factory = getLogEntryFactory();
        if (factory != null) {
            try {
                logEntry = factory.computeLogEntryFrom(doc);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return logEntry;
    }

    public LogEntryFactory getLogEntryFactory() {
        Class<LogEntryFactory> klass = logEntryFactoryKlass;
        LogEntryFactory factory = null;
        if (klass != null) {
            try {
                factory = klass.newInstance();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return factory;
    }

}
