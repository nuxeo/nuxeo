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
    
    public final static String TICKET_NAME_KEY = "ticketKey";

    public final static String PROXY_NAME_KEY = "proxyKey";

    public final static String NUXEO_APP_URL_KEY = "appURL";

    public final static String SERVICE_LOGIN_URL_KEY = "serviceLoginURL";

    public final static String SERVICE_VALIDATE_URL_KEY = "serviceValidateURL";

    public final static String PROXY_VALIDATE_URL_KEY = "proxyValidateURL";

    public final static String SERVICE_NAME_KEY = "serviceKey";

    public final static String LOGOUT_URL_KEY = "logoutURL";

    public final static String DEFAULT_CAS_SERVER_KEY = "defaultCasServer";

    // Constant utility class.
    private CAS2Parameters() {
    }

}
