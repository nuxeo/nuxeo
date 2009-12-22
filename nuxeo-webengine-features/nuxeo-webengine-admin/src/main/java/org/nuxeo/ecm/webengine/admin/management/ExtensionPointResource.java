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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.admin.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ExtensionPointResource {

    protected ExtensionPoint xp;
    protected RegistrationInfoImpl ri;

    public ExtensionPointResource(RegistrationInfo ri, ExtensionPoint xp) {
        this.xp = xp;
        this.ri = (RegistrationInfoImpl) ri;
    }

    @GET
    @Produces("application/atomsvc+xml")
    public Object getContributions() {
        List<Extension> xts = new ArrayList<Extension>();
        Set<RegistrationInfoImpl> depsOnMe = ri.getDependsOnMe();
        if (depsOnMe!= null) {
            for (RegistrationInfo dep : depsOnMe) {
                for (Extension xt : dep.getExtensions()) {
                    if (xt.getTargetComponent().getName().equals(ri.getName().getName())) {
                        xts.add(xt);
                    }
                }
            }
        }
        return new TemplateView(this, "xp-contribs.ftl").arg("xp", xp).arg("contribs", xts).arg("ri", ri);
    }

    @GET
    @Path("{id}")
    public Object getContribution(@PathParam("id") String id) {
        return null;
    }

}
