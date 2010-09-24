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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 *
 */
@WebObject(type = "Queues", administrator=Access.GRANT)
public class QueuesObject extends ManagementObject {

    public static  QueuesObject newObject(DefaultObject from) {
        return (QueuesObject) from.newObject("Queues");
    }

    protected QueueLocator locator;

    protected Map<String,QueueManager<?>> queues ;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        locator = Framework.getLocalService(QueueLocator.class);
        queues =new HashMap<String,QueueManager<?>>();
        for (QueueManager<?> manager:locator.getManagers()) {
            queues.put(manager.getName().getSchemeSpecificPart(), manager);
        }
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    public Collection<QueueManager<?>> getQueues() {
        return queues.values();
    }

    @Path("{queue}")
    public Object doDispatch(@PathParam("queue") String name) {
        return QueueObject.newObject(this, queues.get(name));
    }
}
