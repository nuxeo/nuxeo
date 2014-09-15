/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.user.center;

import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * Codec handling a groupname, an optional view and additional request
 * parameters.
 *
 * @since 5.9.6
 */
public class GroupCodec extends
        AbstractUserGroupCodec {

    public static final String PREFIX = "group";

    public static final String DEFAULT_GROUPS_TAB = "USER_CENTER:UsersGroupsHome:GroupsHome";

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public DocumentView getDocumentViewFromUrl(
            String url) {

        return getDocumentViewFromUrl(
                url,
                DEFAULT_GROUPS_TAB,
                "groupname",
                "showGroup");
    }

    @Override
    public String getUrlFromDocumentView(
            DocumentView docView) {
        return getUrlFromDocumentViewAndID(
                docView, "groupname");
    }

}
