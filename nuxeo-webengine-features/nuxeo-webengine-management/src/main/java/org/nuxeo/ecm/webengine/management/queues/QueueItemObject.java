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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueException;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 *
 */
@WebObject(type="QueueItem")
public class QueueItemObject extends ManagementObject {


    protected QueueHandler handler;
    protected QueueManager manager;
    protected QueueItem item;
    private QueueContent handledContent;

    public static QueueItemObject newObject(DefaultObject from, QueueManager manager, QueueItem item) {
        return (QueueItemObject)from.newObject("QueueItem", manager, item);
    }

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        handler = Framework.getLocalService(QueueHandler.class);
        manager = (QueueManager)args[0];
        item = (QueueItem)args[1];
        handledContent = item.getHandledContent();
    }

    public QueueItem getItem() {
        return item;
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @POST
    @Path("@cancel")
    public Object doPostCancel() {
        return doDelete();
    }

    @POST
    @Path("@retry")
    public Object doPostRetry() {
        try {
            handler.handleRetry(handledContent);
        } catch (QueueException e) {
           throw WebException.wrap("Cannot handle " + handledContent.getName(), e);
        }
        return redirect(getPrevious().getPath());
    }

    @DELETE
    public Object doDelete() {
        manager.forgetContent(handledContent);
        return redirect(getPrevious().getPath());
    }

}
