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

import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourceFactory;
import org.nuxeo.runtime.management.ResourceFactoryDescriptor;
import org.nuxeo.runtime.management.ResourcePublisherService;

/**
 * @author matic
 *
 */
public class AuditEventMetricFactory implements ResourceFactory {

    protected Logs auditService;

    protected ResourcePublisherService publisherService;

    public void configure(ResourcePublisherService service,
            ResourceFactoryDescriptor descriptor) {
        publisherService = service;
        auditService = Framework.getLocalService(Logs.class);
    }

    public static String formatQualifiedName(String name) {
        return ObjectNameFactory.formatMetricQualifiedName(
                NXAuditEventsService.NAME, name);
    }

    public static String formatShortcutName(String name) {
        return ObjectNameFactory.formatMetricShortName("event-" + name);
    }

    public static ObjectName getObjectName(String name) {
        return ObjectNameFactory.getObjectName(formatQualifiedName(name));
    }

    protected void doRegisterResource(String name) {
        publisherService.registerResource(formatShortcutName(name),
                formatQualifiedName(name), AuditEventMetricMBean.class,
                new AuditEventMetricMBeanAdapter(auditService, name));
    }

    protected void doUnregisterResource(String name) {
        publisherService.unregisterResource(name, formatQualifiedName(name));
    }

    public void registerResources() {
        for (String name : auditService.getAuditableEventNames()) {
            doRegisterResource(name);
        }
    }

}
