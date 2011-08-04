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

package org.nuxeo.webengine.blogs.adapters;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.webengine.sites.PerspectiveAdapter;

/**
 * Adapter used to change theme perspectives.
 *
 * @author rux
 */
@WebAdapter(name = "perspective", type = "BlogPerspectiveAdapter", targetType = "Document")
@Produces("text/html; charset=UTF-8")
public class BlogPerspectiveAdapter extends PerspectiveAdapter {

    @POST
    @Path("{path}")
    @Override
    public Object changePerspective(@PathParam("path") String perspective) {
        try {
            return super.changePerspective(perspective);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

}
