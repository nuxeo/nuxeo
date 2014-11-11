/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.webengine.sites;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.webengine.sites.utils.SiteConstants;

/**
 * Adapter used to change theme perspectives.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@WebAdapter(name = "perspective", type = "PerspectiveAdapter", targetType = "Document")
@Produces("text/html;charset=UTF-8")
public class PerspectiveAdapter extends DefaultAdapter {

    @POST
    @Path("{path}")
    public Object changePerspective(@PathParam("path") String perspective) {
        try {
            DocumentObject documentObject = (DocumentObject) getTarget();
            ctx.getRequest().setAttribute(SiteConstants.THEME_PERSPECTIVE,
                    perspective);
            return documentObject.doGet();
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

}
