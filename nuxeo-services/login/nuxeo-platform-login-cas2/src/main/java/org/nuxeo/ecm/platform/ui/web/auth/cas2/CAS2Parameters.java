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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

/**
 * @author Thierry Delprat
 * @author Olivier Adam
 * @author M.-A. Darche
 * @author Thierry Martins
 */
public final class CAS2Parameters {

    public static final String TICKET_NAME_KEY = "ticketKey";

    public static final String PROXY_NAME_KEY = "proxyKey";

    public static final String NUXEO_APP_URL_KEY = "appURL";

    public static final String SERVICE_LOGIN_URL_KEY = "serviceLoginURL";

    public static final String SERVICE_VALIDATE_URL_KEY = "serviceValidateURL";

    public static final String PROXY_VALIDATE_URL_KEY = "proxyValidateURL";

    public static final String SERVICE_NAME_KEY = "serviceKey";

    public static final String LOGOUT_URL_KEY = "logoutURL";

    public static final String DEFAULT_CAS_SERVER_KEY = "defaultCasServer";

    public static final String SERVICE_VALIDATOR_CLASS = "serviceValidatorClass";

    public static final String PROXY_VALIDATOR_CLASS = "proxyValidatorClass";

    public static final String PROMPT_LOGIN = "promptLogin";

    public final static String ERROR_PAGE = "errorPage";

    // Constant utility class.
    private CAS2Parameters() {
    }

}
