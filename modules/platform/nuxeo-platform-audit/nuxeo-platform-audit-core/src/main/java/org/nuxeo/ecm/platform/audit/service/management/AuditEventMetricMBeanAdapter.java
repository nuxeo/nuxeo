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

import org.nuxeo.ecm.platform.audit.api.Logs;

/**
 * @author matic
 */
public class AuditEventMetricMBeanAdapter implements AuditEventMetricMBean {

    protected final Logs service;

    protected final String eventName;

    protected AuditEventMetricMBeanAdapter(Logs service, String name) {
        this.service = service;
        this.eventName = name;
    }

    @Override
    public Long getCount() {
        return service.getEventsCount(eventName);
    }

}
