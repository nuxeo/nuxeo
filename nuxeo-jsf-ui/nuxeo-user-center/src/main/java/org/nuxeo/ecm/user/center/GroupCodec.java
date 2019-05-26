/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.user.center;

import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * Codec handling a groupname, an optional view and additional request parameters.
 *
 * @since 6.0
 */
public class GroupCodec extends AbstractUserGroupCodec {

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
    public DocumentView getDocumentViewFromUrl(String url) {

        return getDocumentViewFromUrl(url, DEFAULT_GROUPS_TAB, "groupname", "showGroup");
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        return getUrlFromDocumentViewAndID(docView, "groupname");
    }

}
