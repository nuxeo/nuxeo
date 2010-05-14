/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

/**
 * @author Thierry Delprat
 * @author Olivier Adam
 * @author M.-A. Darche
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

    // Constant utility class.
    protected CAS2Parameters() {
    }

}
