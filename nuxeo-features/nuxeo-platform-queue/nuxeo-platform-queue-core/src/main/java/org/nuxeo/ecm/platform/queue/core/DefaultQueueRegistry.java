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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.queue.api.QueueError;
import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueueNotFoundError;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.ecm.platform.queue.api.QueueProcessor;
import org.nuxeo.ecm.platform.queue.api.QueueRegistry;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class DefaultQueueRegistry implements QueueRegistry, QueueLocator {


    protected static class Entry<C extends Serializable> {
        protected Class<C> contentType;
        protected QueuePersister<C> persister;
        protected QueueProcessor<C> processor;

        protected Entry(Class<C> contentType, QueuePersister<C> persister, QueueProcessor<C> processor) {
            super();
            this.contentType = contentType;
            this.persister =  persister;
            this.processor = processor;
        }
    }

    protected Map<String, Entry<?>> entries = new HashMap<String, Entry<?>>();

    @Override
    public <C extends Serializable> void register(String queueName, Class<C> contentType, QueuePersister<C> persister, QueueProcessor<C> processor) {
        assert contentType != null;
        assert persister != null;
        assert processor != null;
        entries.put(queueName,  new Entry<C>(contentType, persister, processor));
    }

    @SuppressWarnings("unchecked")
    public <C extends Serializable> QueuePersister<C> getPersister(URI name) {
        return (QueuePersister<C>)entry(name).persister;
    }

    @SuppressWarnings("unchecked")
    public <C extends Serializable> QueueProcessor<C> getProcessor(URI name) {
        return (QueueProcessor<C>) entry(name).processor;
    }

    protected <C extends Serializable> Entry<C> entry(URI name) {
        return entry(queueName(name));
    }

    protected String queueName(URI name) {
        assert name.getScheme().equals("nxqueue");
        return name.getSchemeSpecificPart();
    }

    @SuppressWarnings("unchecked")
    protected <C extends Serializable> Entry<C> entry(String queueName) {
        if (!entries.containsKey(queueName)) {
            throw new QueueNotFoundError(queueName);
        }
        return (Entry<C>) entries.get(queueName);
    }

    @Override
    public <C extends Serializable> QueueManager<C> getManager(URI name) {
        Entry<C> entry = entry(name);
        return new DefaultQueueManager<C>(name,  entry.contentType, this, entry.persister, entry.processor);
    }

    @Override
    public List<QueueManager<?>> getManagers() {
        List<QueueManager<?>> managers = new ArrayList<QueueManager<?>>();
        for (String queueName:entries.keySet()) {
            managers.add(getManager(newQueueName(queueName)));
        }
        return managers;
    }

    @Override
    public URI newContentName(String queueName, String contentName) {
        try {
            return new URI("nxqueue", queueName, contentName);
        } catch (URISyntaxException e) {
            throw new QueueError(String.format("Cannot create URI for %s:%s", queueName, contentName));
        }
    }

    @Override
    public URI newQueueName(String queueName) {
        return newContentName(queueName, null);
    }

    @Override
    public URI newContentName(URI name, String contentName) {
        String scheme = name.getScheme();
        assert "nxqueue".equals(scheme);
        try {
            return new URI(name.getScheme(), name.getSchemeSpecificPart(), contentName);
        } catch (URISyntaxException e) {
            throw new QueueError(String.format("Cannot create URI for %s:%s", name, contentName));
        }
    }
}
