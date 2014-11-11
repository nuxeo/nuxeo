/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.spaces.webobject;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * JAX-RS Root Resource class specialized for WebEngine
 * 
 * @author 10044893.
 * 
 */
@WebObject(type = "spaces", facets = "Folderish")
@Produces("text/html; charset=UTF-8")
public class SpacesModuleRoot extends ModuleRoot {

    /**
     * Log4j logger
     */
    private static final Log log = LogFactory.getLog(SpacesModuleRoot.class);

    public List<Univers> getUniversList() throws SpaceException, Exception {
        CoreSession session = getSession();
        List<Univers> universList = Framework.getService(SpaceManager.class).getUniversList(
                session);

        return universList;
    }

    /**
     * Default view ( index.ftl ) - Lists all available universes
     * 
     * @return
     */
    @GET
    public Object doGet() {
        return getView("index");
    }

    /**
     * Load a particuliar universe from its name with spaces API. Redirect to
     * sub-ressources UniversDocumentObject indirectly
     */
    @Path("{universeName}")
    public Object doGetUnivers(@PathParam("universeName") String universeName) {
        try {
            CoreSession session = getSession();
            SpaceManager spaceManager = Framework.getService(SpaceManager.class);
            Univers universe = spaceManager.getUnivers(universeName, session);

            return newObject("Univers", universe);

        } catch (UniversNotFoundException e) {
            throw new WebResourceNotFoundException("No univers " + universeName
                    + "found", e);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    private CoreSession getSession() {
        return WebEngine.getActiveContext().getCoreSession();
    }

    /**
     * Exception handler
     */
    public Response handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            String fileName = "error/error_401.ftl";
            log.info(fileName);
            return Response.status(401).entity(getTemplate(fileName)).build();
        } else if (e instanceof WebResourceNotFoundException) {
            String fileName = "error/error_404.ftl";
            log.info(fileName);
            return Response.status(404).entity(getTemplate(fileName)).build();
        } else {
            log.info("No error handling for class " + e.getClass().getName());
            log.error(e.getMessage(), e);
            return (Response) super.handleError(e);
        }
    }

}
