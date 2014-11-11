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

package org.nuxeo.webengine.blogs;

import static org.nuxeo.webengine.sites.utils.SiteConstants.MIME_TYPE_RSS_FEED;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.webengine.sites.RssAdapter;

/**
 * Adapter used as a RSS feed. The version of the RSS format is 2.0.
 *
 * @author mcedica
 */
@WebAdapter(name = "rss", type = "RssAdapter", targetType = "Document")
@Produces("application/rss+xml;charset=UTF-8")
public class BlogRssAdapter extends RssAdapter {
    @GET
    @Path("rssOnPage")
    @Produces(MIME_TYPE_RSS_FEED)
    public Template getFeed() {
        return super.getFeed();
    }

    @GET
    @Path("rssOnComments")
    @Produces(MIME_TYPE_RSS_FEED)
    public Template getCommentsFeed() {
        return super.getCommentsFeed();
    }

}
