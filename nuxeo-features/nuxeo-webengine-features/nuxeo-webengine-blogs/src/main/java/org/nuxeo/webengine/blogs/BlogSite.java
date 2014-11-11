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

import static org.nuxeo.webengine.blogs.utils.BlogConstants.BLOG_DOC_TYPE;
import static org.nuxeo.webengine.blogs.utils.BlogConstants.BLOG_POST_DOC_TYPE;
import static org.nuxeo.webengine.blogs.utils.BlogConstants.DEFAULT_WEBSITE_THEMEPAGE_VALUE;
import static org.nuxeo.webengine.blogs.utils.BlogConstants.DEFAULT_WEBSITE_THEME_VALUE;
import static org.nuxeo.webengine.blogs.utils.BlogConstants.SEARCH_THEME_PAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.DATE_AFTER;
import static org.nuxeo.webengine.sites.utils.SiteConstants.DATE_BEFORE;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.webengine.sites.Site;

/**
 * Web object implementation corresponding to BlogSite
 *
 * @author rux
 */
@WebObject(type = BLOG_DOC_TYPE, facets = { BLOG_DOC_TYPE })
@Produces("text/html;charset=UTF-8")
public class BlogSite extends Site {

    @Override
    @Path("{path}")
    public Resource traverse(@PathParam("path") String path) {
        return super.traverse(path);
    }

    @Override
    protected String getDefaultSchemaFieldThemeValue() {
        return DEFAULT_WEBSITE_THEME_VALUE;
    }

    @Override
    protected String getDefaultSchemaFieldThemePageValue() {
        return DEFAULT_WEBSITE_THEMEPAGE_VALUE;
    }

    @Override
    protected String getWebSiteDocumentType() {
        return BLOG_DOC_TYPE;
    }

    @Override
    protected String getWebPageDocumentType() {
        return BLOG_POST_DOC_TYPE;
    }

    @Override
    protected String getSearchThemePage() {
        return SEARCH_THEME_PAGE;
    }

    public String getIdForRss() {
        return doc.getId();
    }

    @Override
    protected void setSearchParameters() {
        super.setSearchParameters();
        String year = ctx.getRequest().getParameter("year");
        String month = ctx.getRequest().getParameter("month");
        if (year != null) {
            Calendar c = null;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd", WebEngine.getActiveContext().getLocale());
            if (month != null) {
                c = new GregorianCalendar(new Integer(year).intValue(),
                        new Integer(month).intValue() - 1, 1);
                ctx.setProperty(DATE_AFTER,
                        simpleDateFormat.format(c.getTime()));
                c.add(Calendar.MONTH, 1);
                ctx.setProperty(DATE_BEFORE,
                        simpleDateFormat.format(c.getTime()));
            } else {
                c = new GregorianCalendar(new Integer(year).intValue(),
                        Calendar.JANUARY, 1);
                ctx.setProperty(DATE_AFTER,
                        simpleDateFormat.format(c.getTime()));
                c.add(Calendar.YEAR, 1);
                ctx.setProperty(DATE_BEFORE,
                        simpleDateFormat.format(c.getTime()));
            }

        }
    }

    @Override
    protected String getErrorTemplateName() {
        return "no_blog.ftl";
    }
}
