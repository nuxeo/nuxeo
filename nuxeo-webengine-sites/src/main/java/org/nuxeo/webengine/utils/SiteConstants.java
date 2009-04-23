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
 * Contributors:
 *     rdarlea
 */
package org.nuxeo.webengine.utils;

/**
 * Utility class used for registering constants.
 */
public final class SiteConstants {

    private SiteConstants() {
    }

    /**
     * Nuxeo document type names
     */
    public static final String CONTEXTUAL_LINK = "ContextualLink";

    public static final String WORKSPACE = "Workspace";

    public static final String WEBPAGE = "WebPage";

    public static final String WEBCONTAINER_URL = "webc:url";
    public static final String WEBCONATINER_NAME = "webc:name";
    public static final String WEBCONTAINER_WELCOMETEXT = "webc:welcomeText";
    public static final String WEBCONTAINER_WELCOMEMEDIA = "webc:welcomeMedia";
    public static final String WEBCONTAINER_LOGO = "webc:logo";
    public static final String WEBCONTAINER_MODERATION = "webcontainer:moderationType";
    public static final String WEBCONTAINER_BASELINE = "webcontainer:baseline";

    public static final String WEBPAGE_THEME = "webp:theme";
    public static final String WEBPAGE_THEMEPAGE = "webp:themePage";
    public static final String WEBPAGE_EDITOR = "webp:isRichtext";
    public static final String WEBPAGE_PUSHTOMENU = "webp:pushtomenu";
    public static final String WEBPAGE_CONTENT = "webp:content";

    /**
     * Constants used in pages like site or webpage
     */
    public static final String PAGE_TITLE = "pageTitle";

    public static final String WELCOME_TEXT = "welcomeText";

    public static final String PAGE_NAME = "name";

    public static final String SITE_DESCRIPTION = "siteDescription";

    public static final String LAST_PUBLISHED_PAGES = "pages";

    public static final String CONTEXTUAL_LINKS = "contextualLinks";

    public static final String COMMENTS = "comments";

    public static final String NUMBER_COMMENTS = "numberComments";

    public static final String ALL_WEBPAGES = "webPages";

    public static final String WEB_VIEW_FACET = "WebView";

    public static final String RESULTS = "results";

    public static final String DELETED = "deleted";


    public static final String WEB_CONTAINER_FACET = "WebView";

    /**
     * Constants used for Comments
     */
    public static final String PERMISSION_COMMENT = "Comment";

    public static final String PERMISSION_MODERATE = "Moderate";

    public static final String MODERATION_APRIORI = "apriori";

    public static final String MODERATION_APOSTERIORI = "aposteriori";



    public static final String VIEW_PERSPECTIVE = "view";

    public static final String CREATE_PERSPECTIVE = "create";

    public static final String EDIT_PERSPECTIVE = "edit";
}

