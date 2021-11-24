/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLink;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginStartupPage;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple helper class for easy access form the login.jsp page
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class LoginScreenHelper {

    protected static final Log log = LogFactory.getLog(LoginScreenHelper.class);

    /**
     * @since 8.4
     */
    public static final String DEFAULT_STARTUP_PAGE_PATH = "home.html";

    private LoginScreenHelper() {
        // helper class
    }

    public static LoginScreenConfig getConfig() {
        PluggableAuthenticationService authenticationService = getPluggableAuthenticationService();
        return authenticationService == null ? null : authenticationService.getLoginScreenConfig();
    }

    /**
     * Registers and returns a login screen configuration including a single login provider described by the given
     * parameters.
     *
     * @since 10.10
     */
    public static LoginScreenConfig registerSingleProviderLoginScreenConfig(String name, String iconUrl, String link,
            String label, String description, LoginProviderLinkComputer computer) {
        LoginProviderLink provider = new LoginProviderLink(name, iconUrl, link, label, description, computer);
        LoginScreenConfig config = new LoginScreenConfig(provider);
        getPluggableAuthenticationService().registerLoginScreenConfig(config);
        return config;
    }

    /**
     * Unregisters the given login screen configuration.
     *
     * @since 10.10
     */
    public static void unregisterLoginScreenConfig(LoginScreenConfig config) {
        getPluggableAuthenticationService().unregisterLoginScreenConfig(config);
    }

    /**
     * @deprecated since 10.10, use
     *             {@link #registerSingleProviderLoginScreenConfig(String, String, String, String, String, LoginProviderLinkComputer)}
     *             instead
     */
    @Deprecated
    public static void registerLoginProvider(String name, String iconUrl, String link, String label, String description,
            LoginProviderLinkComputer computer) {
        registerSingleProviderLoginScreenConfig(name, iconUrl, link, label, description, computer);
    }

    public static String getValueWithDefault(String value, String defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Returns the startup page URL according to the path returned by {@link #getStartupPagePath()}.
     *
     * @since 8.4
     */
    public static String getStartupPageURL(HttpServletRequest request) {
        return VirtualHostHelper.getBaseURL(request) + getStartupPagePath();
    }

    /**
     * Returns the path of the startup page depending on the {@link LoginScreenConfig}/{@link LoginStartupPage}
     * contributions to the {@code loginScreen} extension point.
     *
     * @since 8.4
     */
    public static String getStartupPagePath() {
        LoginScreenConfig config = getConfig();
        if (config == null) {
            log.debug("No <loginScreenConfig> contribution found, startup page path = " + DEFAULT_STARTUP_PAGE_PATH);
            return DEFAULT_STARTUP_PAGE_PATH;
        }
        LoginStartupPage defaultStartupPage = getDefaultStartupPage(config);
        log.debug("Default <startupPage> contribution: " + defaultStartupPage);
        // No <startupPage> contributions, return home.html
        if (defaultStartupPage == null) {
            log.debug("No <startupPage> contribution found, startup page path = " + DEFAULT_STARTUP_PAGE_PATH);
            return DEFAULT_STARTUP_PAGE_PATH;
        }
        // Return the path of the <startupPage> contribution with the highest priority
        String startupPagePath = defaultStartupPage.getPath();
        if (startupPagePath.startsWith("/")) {
            startupPagePath = startupPagePath.substring(1);
        }
        log.debug("Startup page path = " + startupPagePath);
        return startupPagePath;
    }

    /**
     * Returns the paths of the startup pages coming from the {@link LoginScreenConfig}/{@link LoginStartupPage}
     * contributions to the {@code loginScreen} extension point.
     *
     * @since 8.10
     */
    public static List<String> getStartupPagePaths() {
        LoginScreenConfig config = getConfig();
        if (config == null) {
            return Collections.emptyList();
        }
        return config.getStartupPages()
                     .values()
                     .stream()
                     .sorted((p1, p2) -> p2.compareTo(p1))
                     .map(LoginStartupPage::getPath)
                     .collect(Collectors.toList());
    }

    /**
     * Returns whether mobile app banner should be display or not. If the {@code displayMobileBanner} request parameter
     * is used, then it overrides the {@link LoginScreenConfig#getDisplayMobileBanner()} configuration. If there is no
     * {@link LoginScreenConfig}, the default value is {@code true}.
     *
     * @since 10.10-HF55
     */
    public static boolean getDisplayMobileBanner(HttpServletRequest request) {
        String displayMobileBannerReqParam = request.getParameter("displayMobileBanner");
        if (StringUtils.isNotBlank(displayMobileBannerReqParam)) {
            return Boolean.parseBoolean(displayMobileBannerReqParam);
        }
        LoginScreenConfig config = getConfig();
        if (config == null) {
            return true;
        }
        return config.getDisplayMobileBanner();
    }

    /**
     * Returns the {@link LoginStartupPage} contribution with the highest priority or {@code null} if none is
     * contributed.
     *
     * @since 8.4
     */
    protected static LoginStartupPage getDefaultStartupPage(LoginScreenConfig config) {
        if (config.getStartupPages().isEmpty()) {
            return null;
        }
        return Collections.max(config.getStartupPages().values());
    }

    protected static PluggableAuthenticationService getPluggableAuthenticationService() {
        return (PluggableAuthenticationService) Framework.getRuntime()
                                                         .getComponent(PluggableAuthenticationService.NAME);
    }

}
