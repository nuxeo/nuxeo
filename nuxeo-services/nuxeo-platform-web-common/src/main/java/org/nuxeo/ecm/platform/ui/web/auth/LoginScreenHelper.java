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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginStartupPage;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.MobileBannerHelper;
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
     * @deprecated since 9.1, use {@link MobileBannerHelper#PROTOCOL_PROPERTY} instead
     */
    @Deprecated
    public static final String NUXEO_PROTOCOL = "nuxeo://";

    /**
     * @since 8.4
     */
    public static final String DEFAULT_STARTUP_PAGE_PATH = "home.html";

    public static LoginScreenConfig getConfig() {
        PluggableAuthenticationService authService = (PluggableAuthenticationService) Framework.getRuntime()
                                                                                               .getComponent(
                                                                                                       PluggableAuthenticationService.NAME);
        if (authService != null) {
            return authService.getLoginScreenConfig();
        }
        return null;
    }

    public static void registerLoginProvider(String name, String iconUrl, String link, String label, String description,
            LoginProviderLinkComputer computer) {

        LoginScreenConfig config = getConfig();
        if (config != null) {
            config.registerLoginProvider(name, iconUrl, link, label, description, computer);
        } else {
            throw new NuxeoException("There is no available LoginScreen config");
        }
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
                     .map(startupPage -> startupPage.getPath())
                     .collect(Collectors.toList());
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

    /**
     * Returns a full URL that can be handled by the mobile applications.
     *
     * @since 8.10
     * @deprecated since 9.1, use {@link MobileBannerHelper#getURLForAndroidApplication(HttpServletRequest)} or
     *             {@link MobileBannerHelper#getURLForIOSApplication(HttpServletRequest)} instead
     */
    @Deprecated
    public static String getURLForMobileApplication(HttpServletRequest request) {
        String baseURL = VirtualHostHelper.getBaseURL(request);
        String requestedUrl = request.getParameter("requestedUrl");
        return getURLForMobileApplication(baseURL, requestedUrl);
    }

    /**
     * Returns a full URL that can be handled by the mobile applications.
     *
     * @since 8.10
     * @deprecated since 9.1, use
     *             {@link MobileBannerHelper#getURLForMobileApplication(String, String, org.nuxeo.ecm.core.api.DocumentModel, String)}
     *             instead
     */
    @Deprecated
    public static String getURLForMobileApplication(String baseURL, String requestedURL) {
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }

        String url = String.format("%s%s", NUXEO_PROTOCOL, baseURL.replaceAll("://", "/"));
        if (StringUtils.isBlank(requestedURL)) {
            return url;
        }

        DocumentViewCodecManager documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        DocumentView docView = documentViewCodecManager.getDocumentViewFromUrl(requestedURL, false, null);
        if (docView != null && docView.getDocumentLocation() != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            String serverName = docLoc.getServerName();
            if (serverName == null) {
                return url;
            }

            url += serverName;
            IdRef idRef = docLoc.getIdRef();
            PathRef pathRef = docLoc.getPathRef();
            if (idRef != null) {
                url += "/id/" + idRef;
            } else if (pathRef != null) {
                url += "/path" + pathRef;
            }
        }

        return url;
    }

}
