/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.jaxrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

import com.unboundid.scim.data.AuthenticationScheme;
import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.data.ChangePasswordConfig;
import com.unboundid.scim.data.FilterConfig;
import com.unboundid.scim.data.PatchConfig;
import com.unboundid.scim.data.ServiceProviderConfig;
import com.unboundid.scim.data.SortConfig;
import com.unboundid.scim.data.XmlDataFormatConfig;
import com.unboundid.scim.schema.CoreSchema;

/**
 * The root entry for the WebEngine module.
 *
 * @author tiry
 * @since 7.4
 */
@Path("/scim/v1")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "SCIMRoot")
public class SCIMRoot extends ModuleRoot {

    @Path("/Users")
    public Object doGetUsersResource() {
        return newObject("users");
    }

    @Path("/Users.json")
    public Object doGetUsersJsonResource() {
        return newObject("users", MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("/Users.xml")
    public Object doGetUsersXmlResource() {
        return newObject("users", MediaType.APPLICATION_XML_TYPE);
    }

    @Path("/Groups")
    public Object doGetGroups() {
        return newObject("groups");
    }

    @Path("/Groups.json")
    public Object doGetGroupsAsJson() {
        return newObject("groups", MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("/Groups.xml")
    public Object doGetGroupsAsJXml() {
        return newObject("groups", MediaType.APPLICATION_XML_TYPE);
    }

    protected Object getSchema(String schemaName, String format) {

        String viewName = "user-schema";

        if (schemaName.equalsIgnoreCase("users")) {
            viewName = "user-schema";
        } else if (schemaName.equalsIgnoreCase("groups")) {
            viewName = "group-schema";
        }

        Template tmpl = getView(viewName + "." + format);
        return tmpl;
    }

    @GET
    @Path("/Schemas/{schemaName}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + "; qs=0.9" })
    public Object getSchema(@PathParam("schemaName") String schemaName, @Context HttpHeaders headers) {

        List<String> accepted = headers.getRequestHeader("Accept");

        if (accepted.contains(MediaType.APPLICATION_JSON)) {
            return getSchema(schemaName, "json");
        }
        return getSchema(schemaName, "xml");
    }

    @GET
    @Path("/Schemas/{schemaName}.json")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object getSchemaAsJson(@PathParam("schemaName") String schemaName) {
        return getSchema(schemaName, "json");
    }

    @GET
    @Path("/Schemas/{schemaName}.xml")
    @Produces({ MediaType.APPLICATION_XML })
    public Object getSchemaAsXml(@PathParam("schemaName") String schemaName) {
        return getSchema(schemaName, "xml");
    }

    @GET
    @Path("/ServiceProviderConfigs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + "; qs=0.9" })
    public ServiceProviderConfig getConfig() {

        ServiceProviderConfig config = new ServiceProviderConfig(CoreSchema.SERVICE_PROVIDER_CONFIG_SCHEMA_DESCRIPTOR);

        config.setId("Nuxeo");
        config.setExternalId("Nuxeo");

        // auth config
        Collection<AuthenticationScheme> authSchemes = new ArrayList<>();
        authSchemes.add(AuthenticationScheme.createBasic(true));
        config.setAuthenticationSchemes(authSchemes);

        // Filter
        FilterConfig filterConfig = new FilterConfig(true, 1000);
        config.setFilterConfig(filterConfig);

        // Bulk Config : for now
        BulkConfig bulkConfig = new BulkConfig(false, 0, 0);
        config.setBulkConfig(bulkConfig);

        // Pwd
        ChangePasswordConfig changePasswordConfig = new ChangePasswordConfig(false);
        config.setChangePasswordConfig(changePasswordConfig);

        config.setPatchConfig(new PatchConfig(false));

        config.setSortConfig(new SortConfig(true));

        config.setXmlDataFormatConfig(new XmlDataFormatConfig(true));

        return config;
    }

    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity("not authorized").type("text/plain").build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(e.getMessage()).type("text/plain").build();
        } else {
            return super.handleError(e);
        }
    }
}
