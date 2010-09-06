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
 *     mcedica
 */
package org.nuxeo.ecm.webengine.management.statuses;

import javax.ws.rs.GET;
import javax.ws.rs.POST;

import org.nuxeo.ecm.core.management.statuses.ProbeInfo;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;


@WebObject(type = "Probe")
public class ProbeObject extends DefaultObject {

    private ProbeInfo info;

    public static ProbeObject newProbe(DefaultObject parent, ProbeInfo info) {
        return (ProbeObject)parent.newObject("Probe", info);
    }

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length  == 1;
        info = (ProbeInfo) args[0];
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @POST
    public Object run() {
        info.run();
        return redirect(getPath());
    }

    public ProbeInfo getInfo() {
        return info;
    }

}
