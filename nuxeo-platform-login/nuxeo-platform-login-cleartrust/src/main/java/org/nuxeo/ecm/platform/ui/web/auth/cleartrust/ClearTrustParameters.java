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
