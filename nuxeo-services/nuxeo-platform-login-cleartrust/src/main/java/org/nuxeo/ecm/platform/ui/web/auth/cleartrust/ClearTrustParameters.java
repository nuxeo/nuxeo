/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ClearTrustParameters.java 32994 2009-02-11 02:09:57Z madarche $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cleartrust;

/**
 * @author M.-A. Darche
 */
public final class ClearTrustParameters {

    public static final String COOKIE_DOMAIN = "cookieDomain";

    public static final String CLEARTRUST_LOGIN_URL = "cleartrustLoginUrl";

    public static final String CLEARTRUST_LOGOUT_URL = "cleartrustLogoutUrl";

    // Constant utility class
    private ClearTrustParameters() {
    }

}
