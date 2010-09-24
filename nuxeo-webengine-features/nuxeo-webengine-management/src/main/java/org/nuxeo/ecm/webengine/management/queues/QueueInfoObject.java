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

import java.io.Serializable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author matic
 *
 */
@WebObject(type="QueueInfo")
public class QueueInfoObject<C extends Serializable> extends ManagementObject {

    protected QueueManager<C> manager;
    protected QueueInfo<?> info;

    @SuppressWarnings("unchecked")
    public static <C extends Serializable> QueueInfoObject<C> newObject(DefaultObject from, QueueManager<C> manager, QueueInfo<C> info) {
        return (QueueInfoObject<C>)from.newObject("QueueItem", manager, info);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        manager = (QueueManager<C>)args[0];
        info = (QueueInfo<C>)args[1];
    }

    public QueueInfo<?> getInfo() {
        return info;
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @GET
    @Path("@blacklist")
    public Object doBlacklist() {
        info.blacklist();
        return redirect(getPrevious().getPath());
    }

    @GET
    @Path("@retry")
    public Object doRetry() {
        info.retry();
        return redirect(getPrevious().getPath());
    }

}
