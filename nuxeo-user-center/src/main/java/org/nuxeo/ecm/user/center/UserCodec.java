/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.user.center;

import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * Codec handling a username, an optional view and additional request parameters.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class UserCodec extends AbstractUserGroupCodec {

    public static final String PREFIX = "user";

    public static final String DEFAULT_USERS_TAB = "USER_CENTER:UsersGroupsHome:UsersHome";

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        return getDocumentViewFromUrl(url, DEFAULT_USERS_TAB, "username", "showUser");
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        return getUrlFromDocumentViewAndID(docView, "username");
    }

}
