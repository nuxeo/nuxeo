/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.audit.service.management;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.management.ResourcePublisherService;

/**
 * @author matic
 * 
 */
public class AuditEventMetricFactory {

    protected final NXAuditEventsService auditService;

    protected ResourcePublisherService publisherService;

    public AuditEventMetricFactory(NXAuditEventsService auditService) {
        this.auditService = auditService;
    }

    public static String formatQualifiedName(String name) {
        String qualifiedName = ObjectNameFactory.formatMetricQualifiedName(
                NXAuditEventsService.NAME, name);
        return qualifiedName;
    }

    public static String formatShortcutName(String name) {
        return ObjectNameFactory.formatMetricShortName("event-" + name);
    }

    public static ObjectName getObjectName(String name) {
        return ObjectNameFactory.getObjectName(formatQualifiedName(name));
    }

    public boolean checkForPublisher() {
        if (publisherService != null) return true;
        publisherService = (ResourcePublisherService)Framework.getLocalService(ResourcePublisher.class);
        return publisherService != null;
    }
    
    protected static Log log = LogFactory.getLog(AuditEventMetricFactory.class);
    
    public void unregisterResource(String name) {
        if (publisherService == null) return;
        publisherService.unregisterResource(name, formatQualifiedName(name));
    }


    public void registerResource(String eventName) {
        if (checkForPublisher()) {
            log.warn("no resource publisher available for publishing metric of event " + eventName);
        }
        publisherService.registerResource(formatShortcutName(eventName),
                formatQualifiedName(eventName), AuditEventMetricMBean.class,
                new AuditEventMetricMBeanAdapter(auditService, eventName));
    }
}
