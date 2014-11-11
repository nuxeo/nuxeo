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

package org.nuxeo.webengine.blogs.utils;

/**
 *
 * Utility class used for registering constants.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
public final class BlogConstants {

    private BlogConstants() {
    }

    /**
     * Nuxeo document type names
     */
    public static final String BLOG_DOC_TYPE = "BlogSite";

    public static final String BLOG_POST_DOC_TYPE = "BlogPost";

    /**
     * Schemes and fields.
     */
    public static final String DEFAULT_WEBSITE_THEME_VALUE = "blogs";

    public static final String DEFAULT_WEBSITE_THEMEPAGE_VALUE = "site";

    public static final String DEFAULT_WEBPAGE_THEME_VALUE = "blogs";

    public static final String DEFAULT_WEBPAGE_THEMEPAGE_VALUE = "post";

    /**
     * Themes&Perspectives
     */
    public static final String BLOGS_THEME_PAGE = "blogs/sites";

    public static final String SEARCH_THEME_PAGE = "blogs/search";

}
