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
package org.nuxeo.ecm.webengine.management.locks;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.nuxeo.ecm.platform.lock.api.LockCoordinator;
import org.nuxeo.ecm.platform.lock.api.LockInfo;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author matic
 *
 */
@WebObject(type = "Lock")
public class LockObject extends ManagementObject {

    protected LockCoordinator coordinator;

    protected LockInfo info;

    public static LockObject newLock(DefaultObject from, LockInfo info) {
        return (LockObject) from.newObject("Lock", info);
    }

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        coordinator = Framework.getLocalService(LockCoordinator.class);
        info = (LockInfo)args[0];
    }

    public LockInfo getInfo() {
        return info;
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @DELETE
    public Object doDelete() {
        try {
            coordinator.unlock(info.getOwner(), info.getResource());
        } catch (Exception e) {
            throw WebException.wrap("Cannot unlock " + info.getResource(), e);
        }
        return LocksObject.newObject(this);
    }

    @POST
    @Path("@unlock")
    public Object doPostDelete() {
        return doDelete();
    }

}
