/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.openid;

import javax.servlet.http.HttpServletRequest;

/**
 * Helper class to determine the redirect URI based on the current OpenID provider and HTTP request
 *
 * @since 5.7
 */
public interface RedirectUriResolver {
    String getRedirectUri(OpenIDConnectProvider openIDConnectProvider, HttpServletRequest request);
}
