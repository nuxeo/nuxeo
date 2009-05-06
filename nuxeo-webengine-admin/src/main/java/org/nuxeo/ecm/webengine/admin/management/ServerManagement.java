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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Path("server")
public class ServerManagement {

    @GET
    @Produces("application/atomsvc+xml")
    public Object getServiceDocument() {
        return new TemplateView(this, "servicedoc.ftl");
    }

    @GET
    @Path("html")
    @Produces("text/html")
    public Object getServiceDocumentHtml() {
        return new TemplateView(this, "servicedoc.html.ftl");
    }

    @GET
    @Produces("text/plain")
    @Path("system_properties")
    public Object getSystemProperties() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            sb.append(entry.getKey()).append("=").append(formatPropertyValue(value)).append("\n");
        }
        return sb.toString();
    }

    @POST
    @Path("system_properties")
    public Response setSystemProperty(@QueryParam("key") String key, @QueryParam("value") String value) {
        System.setProperty(key, value);
        return Response.ok(getSystemProperties()).build();
    }

    @GET
    @Produces("text/plain")
    @Path("runtime_properties")
    public Object getRuntimeProperties() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : Framework.getProperties().entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue().toString();
            sb.append(entry.getKey()).append("=").append(formatPropertyValue(value)).append("\n");
        }
        return sb.toString();
    }

    @POST
    @Path("runtime_properties")
    public Response setRuntimeProperty(@QueryParam("key") String key, @QueryParam("value") String value) {
        Framework.getProperties().put(key, value);
        return Response.ok(getRuntimeProperties()).build();
    }

    @GET
    @Path("bundles")
    @Produces("application/atom+xml")
    public Object getBundles() {
        if (Framework.getRuntime() instanceof OSGiRuntimeService) {
            OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();
            Bundle[] bundles = runtime.getBundleContext().getBundles();
            return new TemplateView(this, "bundles.ftl").arg("bundles", bundles);
        } else {
            return Response.status(410); // not supported
        }
    }

    @GET
    @Path("components")
    @Produces("application/atom+xml")
    public Object getComponents() {
        Collection<RegistrationInfo> regs = Framework.getRuntime().getComponentManager().getRegistrations();
        return new TemplateView(this, "components.ftl").arg("components", regs);
    }

    /**
     * Only pure configuration components (i.e. that contain only contributions) can be posted.
     */
    @POST
    @Path("components")
    public Response postComponent() {
        return null;
    }

    @Path("components/{name}")
    public ComponentResource getComponent(@PathParam("name") String name) {
        RegistrationInfo ri = Framework.getRuntime().getComponentManager().getRegistrationInfo(new ComponentName(name));
        return new ComponentResource(ri);
    }

    @POST
    @Path("bundles")
    public Response installBundle() {
        return null;
    }

    @Path("bundles/{symbolicName}")
    public BundleResource getBundle(@PathParam("symbolicName") String name) {
        OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();
        for (Bundle bundle : runtime.getBundleContext().getBundles()) {
            if (bundle.getSymbolicName().equals(name)) {
                return new BundleResource(bundle);
            }
        }
        throw new WebResourceNotFoundException("No such bundle: " + name);
    }

    @GET
    @Produces("application/atom+xml")
    @Path("resources")
    public Object getResourceTypes() {
        Environment env = Environment.getDefault();
        List<File> files = new ArrayList<File>();
        files.add(env.getConfig());
        files.add(new File(env.getHome(), "schemas"));
        files.add(new File(env.getHome(), "extensions"));
        File root = new File(env.getHome(), "resources");
        if (root.isDirectory()) {
            files.addAll(Arrays.asList(root.listFiles()));
        }
        return new TemplateView(this, "resources.ftl").arg("resources", files);
    }

    @Path("resources/{type}")
    public FileContainerResource getResourceContainer(@PathParam("type") String type) {
        File root = FileContainerResource.getContainerRoot(type);
        if (!root.isDirectory()) {
            throw new WebResourceNotFoundException("No such resource cotnainer was found: " + type);
        }
        return new FileContainerResource(root);
    }

    @POST
    @Path("resources")
    public Response createResourceContainer(@QueryParam("type") String type) {
        Environment env = Environment.getDefault();
        File rs = new File(env.getHome(), "resources");
        new File(rs, type).mkdirs();
        return Response.ok().build();
    }

    public RuntimeService getRuntime() {
        return Framework.getRuntime();
    }

    public Environment getEnvironment() {
        return Environment.getDefault();
    }

    public String getComponentSummary(RegistrationInfo ri) {
        return ri.getDocumentation();
    }

    public String getBundleHeader(Bundle bundle, String key) {
        return (String) bundle.getHeaders().get(key);
    }

    public String getBundleFileName(Bundle bundle) {
        return Framework.getRuntime().getBundleFile(bundle).getName();
    }

    public static String formatPropertyValue(String value) {
        int k = value.indexOf('\n');
        if (k == -1) {
            return value;
        }
        StringBuilder buf = new StringBuilder();
        if (k > 0 && value.charAt(k - 1) == '\r') {
            buf.append(value.substring(0, k - 1)).append('\\').append("\r\n");
        } else {
            buf.append(value.substring(0, k)).append('\\').append('\n');
        }
        char[] chars = value.toCharArray();
        boolean cr = false;
        for (int i = k + 1; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case '\r':
                    buf.append('\\').append(c);
                    cr = true;
                    break;
                case '\n':
                    if (!cr) {
                        buf.append('\\').append(c);
                    }
                    cr = false;
                    break;
                default:
                    cr = false;
                    buf.append(c);
                    break;
            }
        }
        return buf.toString();
    }

}
