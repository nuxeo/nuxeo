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

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import org.nuxeo.ecm.platform.queue.api.QueueError;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.ecm.platform.queue.api.QueueProcessor;
import org.nuxeo.ecm.platform.queue.api.QueueRegistry;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class QueueComponent extends DefaultComponent {

    DefaultQueueHandler handler;

    DefaultQueueRegistry registry;

    TransactedServiceProvider provider;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof QueueDescriptor) {
            DefaultQueueRegistry registry = (DefaultQueueRegistry)Framework.getLocalService(QueueRegistry.class);
            QueueDescriptor desc = (QueueDescriptor) contribution;
            Method m = registry.getClass().getMethod("register", String.class, Class.class, QueuePersister.class, QueueProcessor.class);
            m.invoke(registry, desc.name, desc.contentType, desc.newPersister(), desc.newProcessor());
            return;
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (QueueHandler.class.isAssignableFrom(adapter)) {
            return adapter.cast(handler);
        }
        if (QueueRegistry.class.isAssignableFrom(adapter)) {
            return adapter.cast(registry);
        }
        if (QueueLocator.class.isAssignableFrom(adapter)) {
            return adapter.cast(registry);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registry = new DefaultQueueRegistry();
        handler = new DefaultQueueHandler(1000, registry);
        provider = new TransactedServiceProvider(DefaultServiceProvider.getProvider());
        DefaultServiceProvider.setProvider(provider);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        DefaultServiceProvider.setProvider(provider.nextProvider);
        handler = null;
        registry = null;
        provider = null;
        super.deactivate(context);
    }

    public static URI newName(String queueName, String contentName) {
        try {
            return new URI("nxqueue", queueName, contentName);
        } catch (URISyntaxException e) {
           throw new QueueError(String.format("Cannot create URI for %s:%s", queueName, contentName));
        }
    }
}
