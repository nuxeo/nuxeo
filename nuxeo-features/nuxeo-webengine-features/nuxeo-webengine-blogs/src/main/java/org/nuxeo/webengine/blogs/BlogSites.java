/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.webengine.blogs;

import static org.nuxeo.webengine.blogs.utils.BlogConstants.BLOGS_THEME_PAGE;
import static org.nuxeo.webengine.blogs.utils.BlogConstants.BLOG_DOC_TYPE;
import static org.nuxeo.webengine.blogs.utils.BlogConstants.BLOG_POST_DOC_TYPE;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.webengine.sites.Sites;

/**
 * Web object implementation corresponding to blog root.
 *
 * @author rux
 */
@Path("/blogs")
@WebObject(type = "blogs", facets = { "Blogs" })
@Produces("text/html;charset=UTF-8")
public class BlogSites extends Sites {

    @Override
    protected String getThemePage() {
        return BLOGS_THEME_PAGE;
    }

    @Override
    public String getWebSiteDocumentType() {
        return BLOG_DOC_TYPE;
    }

    @Override
    public String getWebSiteObjectTypeName() {
        return BLOG_DOC_TYPE;
    }

    @Override
    public String getWebPageDocumentType() {
        return BLOG_POST_DOC_TYPE;
    }

}
