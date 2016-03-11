/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs.rendition;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * @since 7.2
 */
@WebAdapter(name = RenditionAdapter.NAME, type = "renditionAdapter")
public class RenditionAdapter extends DefaultAdapter {

    public static final String NAME = "rendition";

    @Path("{renditionName:((?:(?!/@).)*)}")
    public Object doGetRendition(@Context Request request, @PathParam("renditionName") String renditionName) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        return newObject("rendition", doc, renditionName);
    }
}
