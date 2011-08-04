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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.webengine.sites.TagAdapter;

/**
 * Adapter used to display all documents for a certain tag.
 *
 * @author rux
 */
@WebAdapter(name = "tag", type = "BlogTagAdapter", targetType = "Document")
@Produces("text/html; charset=UTF-8")
public class BlogTagAdapter extends TagAdapter {

    @GET
    @Path("{id}")
    @Override
    public Object changePerspective(@PathParam("id") String tagId) {
        return super.changePerspective(tagId);
    }

    @POST
    @Path("addTagging")
    @Override
    public Object addTagging() {
        return super.addTagging();
    }

    @GET
    @Path("removeTagging")
    @Override
    public Object removeTagging() {
        return super.removeTagging();
    }

}
