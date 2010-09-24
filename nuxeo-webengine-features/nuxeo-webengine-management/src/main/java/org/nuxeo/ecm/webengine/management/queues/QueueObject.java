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
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
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
public class QueueObject<C extends Serializable> extends ManagementObject {

    protected static Log log = LogFactory.getLog(QueueObject.class);

    protected QueueManager<C> manager;

    protected List<QueueInfo<C>> infos;

    @SuppressWarnings("unchecked")
    public static <C extends Serializable> QueueObject<C> newObject(DefaultObject from, QueueManager<C> manager) {
        return (QueueObject<C>) from.newObject("Queue", manager);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        manager = (QueueManager<C>) args[0];
        infos = manager.listKnownContent();
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @GET
    @Path("@blacklist")
    public Object doGetCancel() {
        for (QueueInfo<C> info : infos) {
            if (!info.isBlacklisted()) {
                info.blacklist();
            }
        }
        return redirect(getPath());
    }

    @GET
    @Path("@retry")
    public Object doGetRetry() {
        for (QueueInfo<C> info : infos) {
            info.retry();
        }
        return redirect(getPath());
    }

    public List<QueueInfo<C>> getInfos() {
        return infos;
    }

    public QueueManager<C> getManager() {
        return manager;
    }

    @Path("{content}")
    public Object doDispatch(@PathParam("content") String name) {
        for (QueueInfo<C> info : infos) {
            if (info.getName().getFragment().equals(name)) {
                return QueueInfoObject.newObject(this, manager, info);
            }
        }
        throw new WebResourceNotFoundException("Couldn't find the content " + name);
    }

}
