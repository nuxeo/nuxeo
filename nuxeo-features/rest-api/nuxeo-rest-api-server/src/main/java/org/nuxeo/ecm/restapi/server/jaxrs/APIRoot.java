/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.server.jaxrs.AutomationResource;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * The root entry for the WebEngine module.
 *
 * @since 5.7.2
 */
@Path("/api/v1{repo : (/repo/[^/]+?)?}")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "APIRoot")
public class APIRoot extends ModuleRoot {

    @Path("/")
    public Object doGetRepository(@PathParam("repo")
    String repositoryParam) throws NoSuchDocumentException {
        if (StringUtils.isNotBlank(repositoryParam)) {
            String repoName = repositoryParam.substring("repo/".length() + 1);
            try {
                ctx.setRepositoryName(repoName);
            } catch (final ClientException e) {
                throw new WebResourceNotFoundException(e.getMessage());
            }

        }
        return newObject("repo");
    }

    @Path("/user")
    public Object doGetUser() {
        return newObject("users");
    }

    @Path("/group")
    public Object doGetGroup() {
        return newObject("groups");
    }

    @Path("/automation")
    public Object getAutomationEndPoint() throws Exception {
        return newObject("automation");
    }

    @Path("/directory")
    public Object doGetDirectory() {
        return newObject("directory");
    }

    @Path("/doc")
    public Object doGetDocumentation() {
        return newObject("doc");
    }

    @Path("/query")
    public Object doQuery() {
        return newObject("query");
    }

    @Path("/config")
    public Object doGetConfig() {
        return newObject("config");
    }
}
