/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.management.events;

import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;

/**
 * Factory for Monitoring MBean
 *
 * @author Thierry Delprat
 */
public class EventMonitoringFactory extends AbstractResourceFactory {

    @Override
    public void registerResources() {
        EventMonitoring instance = new EventMonitoring();
         service.registerResource("EventMonitoring",
                 ObjectNameFactory.formatQualifiedName("EventMonitoring"),
                 EventMonitoringMBean.class, instance);
    }

}
