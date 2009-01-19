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
package org.nuxeo.ecm.platform.management.usecases;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ResourceFactory;
import org.nuxeo.runtime.management.ResourceFactoryDescriptor;
import org.nuxeo.runtime.management.ResourcePublisherService;

/**
 * @author matic
 * 
 */
public class UsecaseFactory implements ResourceFactory {

    protected UsecaseSchedulerService scheduler;

    public void configure(ResourcePublisherService service,
            ResourceFactoryDescriptor descriptor) {
        this.scheduler = (UsecaseSchedulerService)Framework.getLocalService(UsecaseScheduler.class);
        this.scheduler.managementPublisher.setService(service);
    }

    public void registerResources() {
        scheduler.managementPublisher.doPublish();
    }

}
