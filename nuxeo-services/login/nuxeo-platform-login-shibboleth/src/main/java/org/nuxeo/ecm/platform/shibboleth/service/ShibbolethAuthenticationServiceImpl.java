/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.shibboleth.service;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REDIRECT_URL;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ShibbolethAuthenticationServiceImpl extends DefaultComponent implements ShibbolethAuthenticationService {

    public static final String CONFIG_EP = "config";

    protected ShibbolethAuthenticationConfig config;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIG_EP.equals(extensionPoint)) {
            config = (ShibbolethAuthenticationConfig) contribution;
        }
    }

    public ShibbolethAuthenticationConfig getConfig() {
        return config;
    }

    @Override
    public String getLoginURL(String redirectURL) {
        if (config == null || config.getLoginURL() == null) {
            return null;
        }

        Map<String, String> urlParameters = new HashMap<>(1);
        urlParameters.put(config.getLoginRedirectURLParameter(), redirectURL);
        return URIUtils.addParametersToURIQuery(config.getLoginURL(), urlParameters);
    }

    @Override
    public String getLogoutURL(String redirectURL) {
        if (config == null || config.getLogoutURL() == null) {
            return null;
        }

        Map<String, String> urlParameters = new HashMap<>(1);
        urlParameters.put(config.getLogoutRedirectURLParameter(), redirectURL);
        return URIUtils.addParametersToURIQuery(config.getLogoutURL(), urlParameters);
    }

    @Override
    public String getLoginURL(HttpServletRequest request) {
        String redirectUrl = VirtualHostHelper.getRedirectUrl(request);
        request.getSession().setAttribute(REDIRECT_URL, redirectUrl);
        return getLoginURL(redirectUrl);
    }

    @Override
    public String getLogoutURL(HttpServletRequest request) {
        return getLogoutURL((String) request.getSession().getAttribute(REDIRECT_URL));
    }

    @Override
    public String getUserID(HttpServletRequest httpRequest) {
        String idpUrl = httpRequest.getHeader(config.getIdpHeader());
        String uidHeader = config.getUidHeaders().get(idpUrl);
        if (uidHeader == null || readHeader(httpRequest, uidHeader) == null
                || readHeader(httpRequest, uidHeader).isEmpty()) {
            uidHeader = config.getDefaultUidHeader();
        }
        return readHeader(httpRequest, uidHeader);
    }

    @Override
    public Map<String, Object> getUserMetadata(String userIdField, HttpServletRequest httpRequest) {
        Map<String, Object> fieldMap = new HashMap<>(config.fieldMapping.size());
        for (String key : config.getFieldMapping().keySet()) {
            fieldMap.put(config.getFieldMapping().get(key), readHeader(httpRequest, key));
        }
        // Force userIdField to shibb userId value in case of the IdP do
        // not use the same mapping as the default's one.
        fieldMap.put(userIdField, getUserID(httpRequest));
        return fieldMap;
    }

    @Override
    public BiMap<String, String> getUserMetadata() {
        BiMap<String, String> biMap = HashBiMap.create();
        biMap.putAll(config.getFieldMapping());
        return biMap;
    }

    protected String readHeader(HttpServletRequest request, String key) {
        String value = request.getHeader(key);
        if (isNotEmpty(value) && isNotEmpty(config.getHeaderEncoding())) {
            try {
                value = new String(value.getBytes("ISO-8859-1"), config.getHeaderEncoding());
            } catch (UnsupportedEncodingException ignored) {
                // Nothing
            }
        }
        return value;
    }
}
