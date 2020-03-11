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
package org.nuxeo.ecm.platform.management.adapters;

import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.model.ComponentName;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class HttpSessionMetricFactory extends AbstractResourceFactory {

    protected static final HttpSessionMetricAdapter mbeanAdapter = new HttpSessionMetricAdapter();

    @Override
    public void registerResources() {
        String qualifiedName = ObjectNameFactory.formatMetricQualifiedName(new ComponentName("httpSessionListener"),
                "http-session");
        service.registerResource("http-session-metric", qualifiedName, HttpSessionMetricMBean.class, mbeanAdapter);
    }

}
