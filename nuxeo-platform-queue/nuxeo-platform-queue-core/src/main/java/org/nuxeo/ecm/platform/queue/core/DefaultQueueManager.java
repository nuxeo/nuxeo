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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueuePersister;
import org.nuxeo.ecm.platform.queue.api.QueueProcessor;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public  class DefaultQueueManager<C extends Serializable> implements QueueManager<C> {

    protected final URI queueName;

    protected final Class<C> contentType;

    protected final QueueLocator locator;

    protected final QueuePersister<C> persister;

    protected final QueueProcessor<C> processor;

    public DefaultQueueManager(URI queueName, Class<C> contentType, QueueLocator locator, QueuePersister<C> persister, QueueProcessor<C> processor) {
        this.queueName = queueName;
        this.contentType = contentType;
        this.locator = locator;
        this.persister = persister;
        this.processor = processor;
    }

    @Override
    public URI getName() {
        return queueName;
    }

    @Override
    public Class<C> getContentType() {
        return contentType;
    }


    @Override
    public boolean knowsContent(URI name)  {
        return persister.hasContent(name);
    }

    @Override
    public List<QueueInfo<C>> listKnownContent() {
        return persister.listKnownItems();
    }

    public List<QueueInfo<C>> listContentWithState(QueueInfo.State state) {
         List<QueueInfo<C>> selection = new ArrayList<QueueInfo<C>>();
        for (QueueInfo<C> info : persister.listKnownItems()) {
            if (info.getState().equals(state)) {
                selection.add(info);
            }
        }
        return selection;
    }


    @Override
    public List<QueueInfo<C>> listHandledContent() {
        return listContentWithState(QueueInfo.State.Handled);
    }

    @Override
    public List<QueueInfo<C>> listOrphanedContent() {
        return listContentWithState(QueueInfo.State.Orphaned);
    }

        @Override
    public List<QueueInfo<C>> listBlacklistedContent() {
         return listContentWithState(QueueInfo.State.Blacklisted); // TODO use dao instead
    }

    @Override
    public void updateInfos(URI name, C content) {
         persister.updateContent(name, content);
    }

    @Override
    public List<QueueInfo<C>> listOwnedContent(URI owner) {
        return persister.listByOwner(owner);
    }

    @Override
    public int removeOwned(URI owner) {
        return persister.removeByOwner(owner);
    }

    @Override
    public  URI newName(String contentName) {
        return locator.newContentName(queueName, contentName);
    }


    @Override
    public void initialize() {
        persister.createIfNotExist();
    }

    @Override
    public int purgeBlacklisted() {
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.MINUTE, -10);
        Date from = cal.getTime();
        return persister.removeBlacklisted(from);
    }

}
