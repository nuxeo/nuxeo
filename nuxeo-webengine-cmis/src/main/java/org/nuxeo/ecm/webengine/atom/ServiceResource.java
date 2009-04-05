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
package org.nuxeo.ecm.webengine.atom;

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.abdera.AbderaRequest;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * A ServiceResource object is the entry point to an APP server.
 * The resource is using a ServiceInfo configuration object to define
 * the service structure. This information is stateful - it is constructed once
 * when the implementation class of the ServiceResource is loaded.
 *
 * We cannot use another mechanism to do this (like a nuxeo service) because of the class loading restrictions
 * that exists in WebEngine modules.
 * WebEngine module classes may be hot reloaded at runtime - this means you cannot use external singleton services to
 * avoid class cast exceptions after a class reload.
 * By using static members initialized when class is loaded you can solve this limitation.
 *
 * A subclass is usually implementing only the method {@link #createServiceInfo()}
 * that should create the definition of the Atom Service and optionally specify an URL Resolver by attaching it to the service.
 * See {@link ServiceInfo#setUrlResolver(UrlResolver)}
 *
 * <ol>
 * <li> the createServiceInfo method that will be called each time the class is loaded
 * by the web class loader. (so the initialization of the stateful data is done each time a class is reloaded)
 * <li> the createUrlResolver that is used to create an abdera target builder that will be used for that service
 * </ol>
 * TODO: use constants and remove literal strings
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ServiceResource extends DefaultObject {

    private static ServiceInfo info = null;

    /**
     * Create an Atom service definition, including a custom optional {@link UrlResolver} if you don't want the default one.
     * The returned instance will be used for any request so it is a sort of singleton object
     * for this reason you should avoid putting inside references to web objects (per request JAX-RS resources).
     * @return
     */
    public abstract ServiceInfo createServiceInfo();


    @Override
    protected void initialize(Object... args) {
        // register the URL Resolver for this request
        UrlResolver resolver = getServiceInfo().getUrlResolver();
        if (resolver == null) {
            throw new WebException("No URL resolver was specfied for this Atom Service", 500);
        }
        ctx.setProperty(AbderaRequest.URL_RESOLVER_KEY, resolver);
    }

    public ServiceInfo getServiceInfo() {
        if (info == null) {
            // we are synchronizing on the implementation class to ensure
            // atomic access for all ServiceResources of the same type
            synchronized (getClass()) {
                if (info == null) {
                    info = createServiceInfo();
                }
            }
        }
        return info;
    }


    @Path("{segment}")
    public Object dispatch(@PathParam("segment") String segment) {
        WorkspaceInfo ws = getServiceInfo().getWorkspace(segment);
        if (ws == null) {
            throw new WebException(404);
        }
        return ws.getResource(ctx);
    }

    @GET
    public Response doGet() {
        try {
            StringWriter sw = new StringWriter();
            XMLWriter xw = new XMLWriter(sw, 4);
            xw.start();
            getServiceInfo().writeTo(ctx.getURL(), xw);
            xw.end();
            return Response.ok(sw.toString()).type("application/atomsvc+xml").build();
        } catch (IOException e) {
            throw WebException.wrap("Failed to write down the service document", e);
        }
    }

}
