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

import java.net.URL;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComponentResource {

    protected RegistrationInfo ri;

    public ComponentResource(RegistrationInfo ri) {
        this.ri = ri;
    }

    @GET
    @Path("html")
    @Produces("text/html")
    public Object getHtml() {
        return new TemplateView(this, "component.html.ftl").arg("component", ri);
    }

    @GET
    @Produces("application/xml")
    public URL doGet() {
        try {
            DefaultRuntimeContext rc = (DefaultRuntimeContext) ri.getContext();
            Map<String, ComponentName> deployedFiles = rc.getDeployedFiles();
            String name = ri.getName().getName();
            if (deployedFiles != null) {
                for (Map.Entry<String, ComponentName> entry : deployedFiles.entrySet()) {
                    if (name.equals(entry.getValue().getName())) {
                        return new URL(entry.getKey());
                    }
                }
            }
            throw new WebResourceNotFoundException("Component definition not found for " + ri.getName().getName());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("xpoints")
    @Produces("application/atomsvc+xml")
    public Object getExtensionPoints() {
        return new TemplateView(this, "xpoints.ftl").arg("xpoints", ri.getExtensionPoints()).arg("ri", ri);
    }

    @GET
    @Path("contribs")
    @Produces("application/atomsvc+xml")
    public Object getContribs() {
        return new TemplateView(this, "contribs.ftl").arg("contribs", ri.getExtensions()).arg("ri", ri);
    }

    @Path("xpoints/{xpoint}")
    public Object getExtensionPoint(@PathParam("xpoint") String xpoint) {
        for (ExtensionPoint xp : ri.getExtensionPoints()) {
            if (xp.getName().equals(xpoint)) {
                return new ExtensionPointResource(ri, xp);
            }
        }
        throw new WebResourceNotFoundException("No such extension point: " + xpoint);
    }

    @DELETE
    public Object removeComponent() {
        if (ri.isPersistent()) {
            try {
                ((OSGiRuntimeService)Framework.getRuntime()).getComponentPersistence().removeComponent(ri.getName().getName());
            } catch (Exception e) {
                throw WebException.wrap("Failed to create component", e);
            }
        }
        return null;
    }

    @PUT
    public Object switchComponentState() {
        RegistrationInfoImpl rii = (RegistrationInfoImpl)ri;
        try {
            if (rii.isActivated()) {
                rii.deactivate();
            } else {
                rii.activate();
            }
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
        return null;
    }

    public String getSummary() throws Exception {
        return ri.getDocumentation();
    }

}
