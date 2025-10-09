/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml;

/**
 * @since 2025.10
 */
public final class SAMLConstants {

    public static final String HTTP_ATTRIBUTE_SAML_CREDENTIAL = "SAMLCredential";

    public static final String HTTP_ATTRIBUTE_SAML_LOGOUT = "SAMLLogout";

    public static final String HTTP_PARAMETER_SAML_REQUEST = "SAMLRequest";

    public static final String HTTP_PARAMETER_SAML_RESPONSE = "SAMLResponse";

    public static final String HTTP_SESSION_SAML_SESSION = "SAML_SESSION";

    private SAMLConstants() {
        // constants class
    }
}
