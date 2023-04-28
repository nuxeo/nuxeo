/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_ERROR;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.web.common.CookieHelper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.NameIDType;

/**
 * @since 2023.0
 */
public final class SAMLUtils {

    public static final String SAML_SESSION_KEY = "SAML_SESSION";

    private SAMLUtils() {
        // utility class
    }

    public static String newUUID() {
        return "_" + UUID.randomUUID();
    }

    @SuppressWarnings("unchecked")
    public static <T extends SAMLObject> T buildSAMLObject(QName qName) {
        return (T) ConfigurationService.get(XMLObjectProviderRegistry.class)
                                       .getBuilderFactory()
                                       .getBuilderOrThrow(qName)
                                       .buildObject(qName);
    }

    public static String getStartPageURL(ServletRequest request) {
        StringBuilder baseURL = new StringBuilder(VirtualHostHelper.getBaseURL(request));
        if (baseURL.charAt(baseURL.length() - 1) != '/') {
            baseURL.append('/');
        }
        baseURL.append(LoginScreenHelper.getStartupPagePath());
        return baseURL.toString();
    }

    public static void setLoginError(HttpServletRequest request, String messageKey) {
        String msg = I18NUtils.getMessageString("messages", messageKey, null, request.getLocale());
        request.setAttribute(LOGIN_ERROR, msg);
    }

    public static Optional<Cookie> getSAMLHttpCookie(HttpServletRequest request) {
        return Stream.ofNullable(request.getCookies())
                     .flatMap(Stream::of)
                     .filter(c -> SAML_SESSION_KEY.equals(c.getName()))
                     .findFirst();
    }

    public static Optional<SAMLSessionCookie> getSAMLSessionCookie(HttpServletRequest request) {
        return getSAMLHttpCookie(request).map(SAMLSessionCookie::fromCookie);
    }

    public static Optional<SAMLSessionCookie> getSAMLSessionCookie(SAMLCredential credential) {
        if (credential.getSessionIndexes() == null || credential.getSessionIndexes().isEmpty()) {
            return Optional.empty();
        }
        String sessionId = credential.getSessionIndexes().get(0);
        String nameValue = credential.getNameID().getValue();
        String nameFormat = defaultIfBlank(credential.getNameID().getFormat(), NameIDType.UNSPECIFIED);
        return Optional.of(new SAMLSessionCookie(sessionId, nameValue, nameFormat));
    }

    public record SAMLSessionCookie(String sessionId, String nameValue, String nameFormat) {

        public Cookie toCookie(HttpServletRequest request) {
            return CookieHelper.createCookie(request, SAML_SESSION_KEY,
                    String.join("|", sessionId, nameValue, nameFormat));
        }

        public static SAMLSessionCookie fromCookie(Cookie cookie) {
            String[] parts = cookie.getValue().split("\\|");
            return new SAMLSessionCookie(parts[0], parts[1], parts[2]);
        }
    }
}
