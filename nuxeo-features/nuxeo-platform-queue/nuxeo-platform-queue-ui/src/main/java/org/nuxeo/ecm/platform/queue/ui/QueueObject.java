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

import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueueManagerLocator;
import org.nuxeo.ecm.platform.queue.api.QueueNotFoundException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
@WebObject(type = "Queue")
public class QueueObject extends DefaultObject {

    List<QueueItem> queueItem;

    @GET
    public Object doGet() {
        QueueManagerLocator locator = Framework.getLocalService(QueueManagerLocator.class);
        QueueManager queuemgr;
        try {
            queuemgr = locator.locateQueue(getName());
        } catch (QueueNotFoundException e) {
            throw WebException.wrap("Couldn't get the queue", e);
        }
        queueItem = queuemgr.listHandledItems();
        return getView("index");
    }

}
