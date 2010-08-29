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
package org.nuxeo.ecm.platform.management.adapters;

import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.model.ComponentName;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class HttpSessionMetricFactory extends AbstractResourceFactory {

    protected static final HttpSessionMetricAdapter mbeanAdapter =
            new HttpSessionMetricAdapter();

    public void registerResources() {
        String qualifiedName = ObjectNameFactory.formatMetricQualifiedName(
                new ComponentName("httpSessionListener"), "http-session");
        service.registerResource("http-session-metric", qualifiedName,
                HttpSessionMetricMBean.class, mbeanAdapter);
    }

}
