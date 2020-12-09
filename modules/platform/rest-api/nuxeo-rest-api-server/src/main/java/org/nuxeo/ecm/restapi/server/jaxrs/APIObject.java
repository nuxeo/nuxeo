/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.List;

import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @since 11.1
 */
@WebObject(type = "apiObject")
public class APIObject extends DefaultObject {

    @Path("/")
    public Object doGetRepository() {
        return doGetRepository(null);
    }

    @Path("/repo/{repositoryName}")
    public Object doGetRepository(@PathParam("repositoryName") String repositoryName) {
        // initialize repository name
        if (StringUtils.isNotBlank(repositoryName)) {
            try {
                ctx.setRepositoryName(repositoryName);
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
     * @since 11.5
     */
    @Path("/server")
    public Object server() {
        return newObject("server");
    }

    /**
     * @since 7.2
     */
    @Path("/ext/{otherPath}")
    public Object route(@PathParam("otherPath") String otherPath) {
        return newObject(otherPath);
    }
}
