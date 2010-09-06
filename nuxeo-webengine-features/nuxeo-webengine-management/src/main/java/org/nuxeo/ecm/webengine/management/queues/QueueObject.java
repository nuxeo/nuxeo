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
package org.nuxeo.ecm.webengine.management.queues;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author matic
 *
 */
@WebObject(type = "Queue")
public class QueueObject extends ManagementObject {

    protected QueueManager manager;
    protected List<QueueItem> items;

    public static QueueObject newObject(DefaultObject from, QueueManager manager) {
        return (QueueObject)from.newObject("Queue", manager);
    }

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        manager = (QueueManager)args[0];
        items = manager.listHandledItems();
    }



    @GET
    public Object doGet() {
        return getView("index");
    }

    public List<QueueItem> getItems() {
        return items;
    }

    public QueueManager getManager() {
        return manager;
    }

    @Path("{item}")
    public Object doDispatch(@PathParam("item") String name) {
        for (QueueItem queueitem : items) {
            if (queueitem.getHandledContent().getName().equals(name)) {
                return QueueItemObject.newObject(this, manager, queueitem);
            }
        }
        throw new WebResourceNotFoundException("Couldn't find the queue " + name);
    }

}
