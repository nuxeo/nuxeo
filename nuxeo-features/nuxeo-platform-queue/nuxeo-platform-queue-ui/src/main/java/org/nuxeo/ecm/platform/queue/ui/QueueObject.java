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
package org.nuxeo.ecm.platform.queue.ui;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.queue.api.QueueException;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueueManagerLocator;
import org.nuxeo.ecm.platform.queue.api.QueueNotFoundException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
@WebObject(type = "Queue", guard = "group=administrators")
public class QueueObject extends DefaultObject {

    public static final Log log = LogFactory.getLog(QueueObject.class);

    List<QueueItem> queueItems;

    @GET
    public Object doGet() {
        QueueManagerLocator locator = Framework.getLocalService(QueueManagerLocator.class);
        QueueManager queuemgr;
        try {
            queuemgr = locator.locateQueue(getName());
        } catch (QueueNotFoundException e) {
            throw WebException.wrap("Couldn't get the queue", e);
        }
        queueItems = queuemgr.listHandledItems();
        return getView("index");
    }

    public List<QueueItem> getQueueItems() {
        return queueItems;
    }

    @GET
    @Path("{queueItemName}/start")
    public Object relaunch(@PathParam("queueItemName") String name) {
        QueueManagerLocator locator = Framework.getLocalService(QueueManagerLocator.class);
        QueueManager queuemgr;
        try {
            queuemgr = locator.locateQueue(getName());
        } catch (QueueNotFoundException e) {
            throw WebException.wrap("Couldn't get the queue", e);
        }
        queueItems = queuemgr.listHandledItems();
        for (QueueItem queueitem : queueItems) {
            if (queueitem.getHandledContent().getName().equals(name)) {

                QueueHandler handler = Framework.getLocalService(QueueHandler.class);
                try {
                    handler.handleNewContentIfUnknown(queueitem.getHandledContent());
                } catch (QueueException e) {
                    log.error("An error occured while handling content", e);
                    throw WebException.wrap(e);
                }
                return redirect(getPath());
            }
        }
        throw new WebResourceNotFoundException("Couldn't find the queue "
                + name);

    }
}
