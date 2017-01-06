/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth;

public final class NXAuthConstants {

    public static final String PRINCIPAL_KEY = "org.nuxeo.ecm.Principal";

    public static final String USERNAME_KEY = "user_name";

    public static final String PASSWORD_KEY = "user_password";

    public static final String FORM_SUBMITTED_MARKER = "form_submitted_marker";

    public static final String USERIDENT_KEY = "org.nuxeo.ecm.login.identity";

    public static final String LOGINCONTEXT_KEY = "org.nuxeo.ecm.login.context";

    public static final String LOGIN_ERROR = "org.nuxeo.ecm.login.error";

    public static final String LOGIN_STATUS_CODE = "org.nuxeo.ecm.login.status.code";

    public static final String LOGOUT_PAGE = "logout";

    public static final String LOGIN_PAGE = "login";

    public static final String SWITCH_USER_PAGE = "swuser";

    public static final String PAGE_AFTER_SWITCH = "pageAfterSwitch";

    public static final String SWITCH_USER_KEY = "deputy";

    public static final String ERROR_CONNECTION_FAILED = "connection.error";

    public static final String ERROR_AUTHENTICATION_FAILED = "authentication.failed";

    public static final String ERROR_USERNAME_MISSING = "username.missing";

    public static final String FORCE_ANONYMOUS_LOGIN = "forceAnonymousLogin";

    public static final String REQUESTED_URL = "requestedUrl";

    public static final String SECURITY_ERROR = "securityError";

    public static final String LOGIN_MISSING = "loginMissing";

    public static final String LOGIN_CONNECTION_FAILED = "connectionFailed";

    public static final String LOGIN_FAILED = "loginFailed";

    public static final String DISABLE_REDIRECT_REQUEST_KEY = "nuxeo.disable.redirect.wrapper";

    public static final String SSO_INITIAL_URL_REQUEST_KEY = "sso.initial.url.request";

    public static final String START_PAGE_SAVE_KEY = "Nuxeo5_Start_Page";

    public static final String LANGUAGE_PARAMETER = "language";

    public static final String SESSION_TIMEOUT = "nxtimeout";

    /**
     * Name of the session parameter which stores the url to be redirected when logging out
     *
     * @since 9.1
     */
    public static final String REDIRECT_URL = "redirect_url";

    @Deprecated
    // because of typo.
    public static final String PASSORD_KEY = "user_password";

    // Constant utility class.
    private NXAuthConstants() {
    }

}
