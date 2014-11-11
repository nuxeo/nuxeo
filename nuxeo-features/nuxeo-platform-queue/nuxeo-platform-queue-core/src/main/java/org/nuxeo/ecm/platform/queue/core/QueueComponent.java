/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.core;

import org.nuxeo.ecm.platform.queue.api.QueueFactory;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueManagerLocator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class QueueComponent extends DefaultComponent {

    QueueHandler handler;

    QueueFactory factory;

    QueueManagerLocator managerLocator;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof QueueDescriptor) {
            QueueFactory factory = Framework.getLocalService(QueueFactory.class);
            QueueDescriptor desc = (QueueDescriptor) contribution;
            factory.createQueue(desc.name, desc.newPersisterInstance(),
                    desc.newExecutorInstance());
        }

    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (QueueHandler.class.isAssignableFrom(adapter)) {
            return adapter.cast(handler);
        }
        if (QueueFactory.class.isAssignableFrom(adapter)) {
            return adapter.cast(factory);
        }
        if (QueueManagerLocator.class.isAssignableFrom(adapter)) {
            return adapter.cast(managerLocator);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        handler = new QueueHandlerImpl();
        factory = new QueueFactoryImpl();
        managerLocator = new QueueManagerLocatorImpl();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        handler = null;
        factory = null;
        super.deactivate(context);
    }

}
