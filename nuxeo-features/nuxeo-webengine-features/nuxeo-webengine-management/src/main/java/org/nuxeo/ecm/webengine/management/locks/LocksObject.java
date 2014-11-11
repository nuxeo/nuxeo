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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.platform.lock.api.LockInfo;
import org.nuxeo.ecm.platform.lock.api.LockReader;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;


/**
 * @author matic
 *
 */
@WebObject(type = "Locks")
public class LocksObject extends ManagementObject {

    protected LockReader reader;
    protected List<LockInfo> infos;

    public static LocksObject newObject(DefaultObject parent) {
        return (LocksObject)parent.newObject("Locks");
    }

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        reader = Framework.getLocalService(LockReader.class);
        try {
            infos = reader.getInfos();
        } catch (InterruptedException e) {
            throw WebException.wrap("Unavailable infos", e);
        }
    }

    public List<LockInfo> getInfos() {
        return infos;
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("{resource}")
    public ManagementObject dispatch(@PathParam("resource") String resource) {
        for (LockInfo info:infos) {
            if (info.getResource().toASCIIString().equals(resource)) {
                return  LockObject.newLock(this, info);
            }
        }
        throw new WebResourceNotFoundException("No locks on " + resource);
    }

}
