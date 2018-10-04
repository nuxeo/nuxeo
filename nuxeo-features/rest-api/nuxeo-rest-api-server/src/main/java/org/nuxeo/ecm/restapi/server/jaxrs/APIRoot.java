/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     dmetzler
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.List;

import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
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
    public Object doGetRepository(@PathParam("repo") String repositoryParam) throws DocumentNotFoundException {
        if (StringUtils.isNotBlank(repositoryParam)) {
            String repoName = repositoryParam.substring("repo/".length() + 1);
            try {
                ctx.setRepositoryName(repoName);
            } catch (IllegalArgumentException e) {
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
    public Object getAutomationEndPoint() {
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

    @Path("/conversion")
    public Object doGetConversion() {
        return newObject("conversions");
    }

    /**
     * @since 10.3
     */
    @Path("/bulk")
    @SuppressWarnings("deprecation")
    // we need to handle ids matrix because matrix aren't present in path used for dispatch
    public Object bulk(@MatrixParam("id") List<String> ids) {
        if (ids.isEmpty()) {
            return newObject("bulkActionFramework");
        } else {
            return RepositoryObject.getBulkDocuments(this, ids);
        }
    }

    /**
     * @since 7.2
     */
    @Path("/ext/{otherPath}")
    public Object route(@PathParam("otherPath") String otherPath) {
        return newObject(otherPath);
    }
}
