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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleResource {

    protected Bundle bundle;

    public BundleResource(Bundle bundle) {
        this.bundle = bundle;
    }

    @GET
    @Produces("application/xml+atom")
    public Object getDefinition() {
        OSGiRuntimeService runtime = (OSGiRuntimeService)Framework.getRuntime();
        List<RegistrationInfo> comps = new ArrayList<RegistrationInfo>();
        for (RegistrationInfo ri :runtime.getComponentManager().getRegistrations()) {
            if (ri.getContext().getBundle().getSymbolicName().equals(bundle.getSymbolicName())) {
                comps.add(ri);
            }
        }
        return new TemplateView(this, "bundle-components.ftl").arg("components", comps).arg("bundle", bundle);
    }


    @GET
    @Path("file")
    @Produces("application/octet-stream")
    public File getBundleFile() {
        File file = Framework.getRuntime().getBundleFile(bundle);
        return file;
    }


    @GET
    @Path("manifest")
    @Produces("text/plain")
    public Object getManifest() {
        URL url = bundle.getEntry("META-INF/MANIFEST.MF");
        if (url == null) {
            return "";
        }
        InputStream in = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in = url.openStream();
            FileUtils.copy(in, out);
            return new String(out.toByteArray());
        } catch (Exception e) {
            throw WebException.wrap(e);
        } finally {
            try { if (in != null) in.close();} catch (Exception e) {}
        }
    }


    @PUT
    public Response switchBundleState() {
        return Response.ok().build();
    }


    @DELETE
    public Response removeBundle() {
        return Response.ok().build();
    }

}
