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
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Univers ( Nuxeo-spaces-api concept ) web engine object
 **/
@WebObject(type = "Univers")
@Produces("text/html; charset=UTF-8")
public class UniversWebObject extends DefaultObject {

    /**
     * Current universe
     */
    private Univers univers;

    /**
     * all spaces in the current universe
     */
    private List<Space> spaces;

    private static final Log log = LogFactory.getLog(UniversWebObject.class);

    public List<Space> getSpaces() throws Exception {
        if (spaces == null) {
            spaces = Framework.getService(SpaceManager.class).getSpacesForUnivers(
                    univers, getSession());
        }
        return spaces;
    }

    @GET
    public Response doGet() {
        try {
            List<Space> spaces = getSpaces();
            if (!spaces.isEmpty()) {
                return redirect(getPath() + "/" + spaces.get(0).getName());
            } else {
                throw new WebResourceNotFoundException(
                        "No space found for this universe");
            }
        } catch (Exception e) {
            throw new WebResourceNotFoundException(e.getMessage(), e);
        }

    }

    public Univers getUnivers() {
        return univers;
    }

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        univers = (Univers) args[0];

    }

    /**
     * Reads a space with spaces API.
     */
    @Path("{spacename}")
    public Resource doGetSpace(@PathParam("spacename") String spacename) {
        getContext().getRequest().setAttribute("currentUnivers", univers);
        try {

            CoreSession coreSession = getSession();
            SpaceManager spaceManager = Framework.getService(SpaceManager.class);

            Space space = spaceManager.getSpace(spacename, univers,
                    coreSession);
            if (space == null) {
                throw new WebResourceNotFoundException("No space " + spacename
                        + " found");
            }
            return newObject("Space", space);

        } catch (SpaceNotFoundException e) {
            throw new WebResourceNotFoundException("No space " + spacename
                    + " found for this universe");
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    private CoreSession getSession() {
        return WebEngine.getActiveContext().getCoreSession();
    }

}
