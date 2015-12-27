/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
