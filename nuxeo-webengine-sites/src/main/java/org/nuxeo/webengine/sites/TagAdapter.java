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

import static org.nuxeo.webengine.sites.utils.SiteConstants.SEARCH_THEME_PAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.TAG_DOCUMENT;
import static org.nuxeo.webengine.sites.utils.SiteConstants.THEME_BUNDLE;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * 
 * Adapter used to display all documents for a certain tag.
 * 
 * @author mcedica
 */
@WebAdapter(name = "tag", type = "TagAdapter", targetType = "Document")
@Produces("text/html; charset=UTF-8")
public class TagAdapter extends DefaultAdapter {

    @GET
    @Path(value = "{id}")
    public Object changePerspective(@PathParam("id") String tagId) {
        try {
            DocumentObject documentObject = (DocumentObject) getTarget();
            ctx.getRequest().setAttribute(THEME_BUNDLE, SEARCH_THEME_PAGE);
            ctx.setProperty(TAG_DOCUMENT, tagId);
            if (documentObject instanceof Site) {
                return getTemplate("template_default.ftl").args(
                        ((Site) documentObject).getSiteArguments());
            }
            if (documentObject instanceof Page) {
                return getTemplate("template_default.ftl").args(
                        ((Page) documentObject).getPageArguments());
            }
            return getTemplate("template_default.ftl");
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }
}
