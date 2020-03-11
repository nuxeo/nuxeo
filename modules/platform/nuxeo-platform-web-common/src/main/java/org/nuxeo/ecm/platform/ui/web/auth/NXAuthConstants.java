/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    /**
     * @since 9.3
     */
    public static final String START_PAGE_FRAGMENT_KEY = "nuxeo.start.url.fragment";

    public static final String START_PAGE_SAVE_KEY = "Nuxeo5_Start_Page";

    /**
     * @deprecated since 9.3
     */
    @Deprecated
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

    /**
     * @since 10.3
     */
    protected static final String CALLBACK_URL_PARAMETER = "callbackURL";

    /**
     * @since 10.3
     */
    protected static final String MOBILE_PROTOCOL = "nuxeo://";

    /**
     * @since 10.3
     */
    protected static final String DRIVE_PROTOCOL = "nxdrive://";

    // Constant utility class.
    private NXAuthConstants() {
    }

}
