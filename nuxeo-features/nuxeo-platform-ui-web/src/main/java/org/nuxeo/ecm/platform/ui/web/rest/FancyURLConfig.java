/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest;

/**
 * Stores URL generation configuration. Should be externalised to an
 * ExtensionPoint or the Runtime configuration service.
 *
 * @author tiry
 * @deprecated externalized to an extension point
 */
@Deprecated
public final class FancyURLConfig {

    // enables Zope-like URL parsing
    public static final Boolean ENABLE_FANCY_URL_PARSING = true;

    // rewrite all URLs to Zope-Like URL
    public static final Boolean ENABLE_FANCY_URL_REDIRECT = true;

    // prefix for fancy urls
    public static final String FANCY_URL_PREFIX = "/nxdoc";

    // parameters used to transmit context via GET
    public static final String GET_URL_Server_Param = "repositoryName";

    public static final String GET_URL_Doc_Param = "documentId";

    public static final String GET_URL_Tab_Param = "currentTab";

    public static final String DEFAULT_VIEW_ID = "view_documents";

    public static final String DEFAULT_TAB_NAME = "default";

    public static final Boolean USE_FANCY_URL = true;

    public static final Boolean NEED_BASE_URL = true;

    // Constant utility class.
    private FancyURLConfig() {
    }

}
