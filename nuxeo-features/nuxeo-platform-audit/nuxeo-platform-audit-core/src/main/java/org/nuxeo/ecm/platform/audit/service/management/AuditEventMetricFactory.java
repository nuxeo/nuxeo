/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
public class AuditEventMetricFactory implements ResourceFactory {

    protected Logs auditService;

    protected ResourcePublisherService publisherService;

    public void configure(ResourcePublisherService service, ResourceFactoryDescriptor descriptor) {
        publisherService = service;
        auditService = Framework.getService(Logs.class);
    }

    public static String formatQualifiedName(String name) {
        return ObjectNameFactory.formatMetricQualifiedName(NXAuditEventsService.NAME, name);
    }

    public static String formatShortcutName(String name) {
        return ObjectNameFactory.formatMetricShortName("event-" + name);
    }

    public static ObjectName getObjectName(String name) {
        return ObjectNameFactory.getObjectName(formatQualifiedName(name));
    }

    protected void doRegisterResource(String name) {
        publisherService.registerResource(formatShortcutName(name), formatQualifiedName(name),
                AuditEventMetricMBean.class, new AuditEventMetricMBeanAdapter(auditService, name));
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
